import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

/**
 To hell with the clickmaster shitass achievement lol

 Credits: HeavyFromTF2 on GitHub \o/
 */
public class ZonewallWTTG2 extends JPanel implements ActionListener, MouseListener {
    static final int WIDTH = 900;
    static final int HEIGHT = 300;
    private final int MARGIN_LEFT = 40;
    private final int MARGIN_RIGHT = 40;
    private final int TRACK_Y = 60;
    private final int POINT_SIZE = 14;

    private final int SLOTS = 30;
    private int redCount = 3;      // Number of red slots
    private int redStart = 10;     // Start index of red slots

    private double barX;           // Current X position of the moving bar
    private double barSpeed = 6.0; // Speed of the moving bar in pixels per timer tick
    private final int BAR_PIXEL_WIDTH = 8; // Width of the moving bar
    private boolean movingRight = true;     // Direction flag for bar movement

    private Timer timer;           // Swing timer controlling animation
    private boolean running = false;   // Game running state flag
    private Random rnd = new Random();
    private int tries = 0;          // Number of clicks made
    private int successes = 0;      // Number of successful clicks (on red)
    private int consecutive = 0;    // Consecutive successful clicks counter

    private JLabel statusLabel;     // Label to show status messages

    // Feedback message text, color, and expiration time
    private String feedbackText = "";
    private Color feedbackColor = Color.WHITE;
    private long feedbackEndTime = 0;

    // Constructor: set up JPanel and timer, initialize positions
    public ZonewallWTTG2() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        addMouseListener(this);
        timer = new Timer(16, this); // roughly 60 FPS animation timer
        resetPositions();
    }

    // Reset game state: position bar, choose new red slots, reset counters and feedback
    private void resetPositions() {
        barX = MARGIN_LEFT;
        movingRight = true;
        redStart = rnd.nextInt(SLOTS - redCount + 1); // random start for red segment
        tries = 0;
        successes = 0;
        consecutive = 0;
        feedbackText = "";
    }

    // Calculate center X coordinate of a given slot index on the track
    private double slotCenterX(int slotIndex) {
        double trackWidth = WIDTH - MARGIN_LEFT - MARGIN_RIGHT;
        double slotWidth = trackWidth / (double) SLOTS;
        return MARGIN_LEFT + slotWidth * slotIndex + slotWidth / 2.0;
    }

    /**
     * Determine current slot index the bar is over.
     *
     * NOTE: changed to "nearest-center" method to avoid off-by-one errors
     * that happen when using rounding at slot boundaries (especially when the
     * bar has width > 1 and moves fast). This compares the center of the bar
     * with the centers of all slots and picks the closest one.
     */
    private int currentSlotIndex() {
        double barCenter = barX + BAR_PIXEL_WIDTH / 2.0;
        int bestIdx = 0;
        double bestDist = Double.MAX_VALUE;

        for (int i = 0; i < SLOTS; i++) {
            double cx = slotCenterX(i);
            double dist = Math.abs(cx - barCenter);
            if (dist < bestDist) {
                bestDist = dist;
                bestIdx = i;
            }
        }
        // clamp just in case
        if (bestIdx < 0) bestIdx = 0;
        if (bestIdx > SLOTS - 1) bestIdx = SLOTS - 1;
        return bestIdx;
    }

    // Paint method: draw all elements including track, slots, bar, and info text
    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double trackWidth = WIDTH - MARGIN_LEFT - MARGIN_RIGHT;
        double slotWidth = trackWidth / (double) SLOTS;

        // Draw white square brackets delimiting the track
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 36));
        g.drawString("[", MARGIN_LEFT - 25, TRACK_Y + 30);
        g.drawString("]", WIDTH - MARGIN_RIGHT, TRACK_Y + 30);

        // Draw all slots as circles: red slots in red, others in dark green
        for (int i = 0; i < SLOTS; i++) {
            double cx = slotCenterX(i);
            int x = (int) Math.round(cx - POINT_SIZE / 2.0);
            int y = TRACK_Y;
            if (i >= redStart && i < redStart + redCount) {
                g.setColor(Color.RED);
            } else {
                g.setColor(new Color(0, 100, 0)); // dark green for other slots
            }
            g.fillOval(x, y, POINT_SIZE, POINT_SIZE);
            g.setColor(Color.BLACK);
            g.drawOval(x, y, POINT_SIZE, POINT_SIZE);
        }

        // Draw the moving bar with semi-transparent light blue fill and cyan border
        int bx = (int) Math.round(barX);
        g.setColor(new Color(180, 220, 255, 200));
        g.fillRect(bx, TRACK_Y - 10, BAR_PIXEL_WIDTH, 80);
        g.setColor(Color.CYAN);
        g.drawRect(bx, TRACK_Y - 10, BAR_PIXEL_WIDTH, 80);

        // Draw informational text at bottom
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.setColor(Color.WHITE);
        g.drawString("Tries: " + tries, 20, HEIGHT - 40);
        g.drawString("Successes: " + successes, 120, HEIGHT - 40);
        g.drawString("Consecutive: " + consecutive, 260, HEIGHT - 40);
        g.drawString(String.format("Speed: %.2f px/tick", barSpeed), 420, HEIGHT - 40);
        g.drawString("Red count: " + redCount, 620, HEIGHT - 40);

        // Draw feedback text (success or fail) centered if active
        if (!feedbackText.isEmpty() && System.currentTimeMillis() < feedbackEndTime) {
            g.setFont(new Font("SansSerif", Font.BOLD, 36));
            g.setColor(feedbackColor);
            FontMetrics fm = g.getFontMetrics();
            int textWidth = fm.stringWidth(feedbackText);
            g.drawString(feedbackText, (WIDTH - textWidth) / 2, HEIGHT / 2);
        }

        g.dispose();
    }

    // Timer callback: update bar position and direction, then repaint
    @Override
    public void actionPerformed(ActionEvent e) {
        if (!running) return;
        if (movingRight) {
            barX += barSpeed;
            if (barX >= WIDTH - MARGIN_RIGHT - BAR_PIXEL_WIDTH) {
                barX = WIDTH - MARGIN_RIGHT - BAR_PIXEL_WIDTH;
                movingRight = false;
            }
        } else {
            barX -= barSpeed;
            if (barX <= MARGIN_LEFT) {
                barX = MARGIN_LEFT;
                movingRight = true;
            }
        }
        repaint();
    }

    // Mouse click handler: check if click was on red slot, update stats, play beep, show feedback
    @Override
    public void mouseClicked(MouseEvent e) {
        if (!running) return;
        tries++;
        int idx = currentSlotIndex();
        if (idx >= redStart && idx < redStart + redCount) {
            successes++;
            consecutive++;
            // Play quick double beep sound for success
            Toolkit.getDefaultToolkit().beep();
            new Thread(() -> {
                try { Thread.sleep(60); } catch (InterruptedException ignored) {}
                Toolkit.getDefaultToolkit().beep();
            }).start();
            showFeedback("SUCCESS!", Color.GREEN);
            statusLabel.setText("SUCCESS! Slot " + idx + " red. Consecutive: " + consecutive);
            // Choose new red segment position
            redStart = rnd.nextInt(SLOTS - redCount + 1);
        } else {
            consecutive = 0;
            showFeedback("FAIL!", Color.RED);
            statusLabel.setText("FAIL. Slot " + idx + " not red. Reset consecutive.");
        }
        repaint();
    }

    // Helper to display feedback message for 1 second
    private void showFeedback(String text, Color color) {
        feedbackText = text;
        feedbackColor = color;
        feedbackEndTime = System.currentTimeMillis() + 1000;
    }

    // Unused mouse event handlers
    @Override public void mousePressed(MouseEvent e) {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}

    // Setup GUI frame, panels, buttons and their event listeners
    private static void createAndShowGui() {
        JFrame frame = new JFrame("ZoneWall 40 slots - var red count");
        ZonewallWTTG2 game = new ZonewallWTTG2();

        JPanel leftPanel = new JPanel();
        JButton startBtn = new JButton("Start");
        JButton stopBtn = new JButton("Stop");
        JButton resetBtn = new JButton("Reset");
        JLabel status = new JLabel("Click Start to begin.");
        game.statusLabel = status;
        leftPanel.add(startBtn);
        leftPanel.add(stopBtn);
        leftPanel.add(resetBtn);

        JPanel rightPanel = new JPanel();
        JButton lessRed = new JButton("- Red");
        JButton moreRed = new JButton("+ Red");
        JButton harderBtn = new JButton("Harder");
        JButton easierBtn = new JButton("Easier");
        rightPanel.add(lessRed);
        rightPanel.add(moreRed);
        rightPanel.add(harderBtn);
        rightPanel.add(easierBtn);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(leftPanel, BorderLayout.WEST);
        JPanel centerPanel = new JPanel();
        centerPanel.add(status);
        topPanel.add(centerPanel, BorderLayout.CENTER);
        topPanel.add(rightPanel, BorderLayout.EAST);

        // Start button starts the timer and sets running flag
        startBtn.addActionListener(a -> {
            if (!game.running) {
                game.running = true;
                game.timer.start();
                status.setText("Running. Click when bar is on red slot.");
            }
        });

        // Stop button stops the timer and clears running flag
        stopBtn.addActionListener(a -> {
            game.running = false;
            game.timer.stop();
            status.setText("Stopped.");
        });

        // Reset button stops timer and resets positions and counters
        resetBtn.addActionListener(a -> {
            game.running = false;
            game.timer.stop();
            game.resetPositions();
            status.setText("Reset. New red start: " + game.redStart);
            game.repaint();
        });

        // Increase bar speed (up to max 24)
        harderBtn.addActionListener(a -> {
            game.barSpeed = Math.min(24.0, game.barSpeed + 1.5);
            status.setText("Speed increased: " + String.format("%.2f", game.barSpeed));
        });

        // Decrease bar speed (down to min 1)
        easierBtn.addActionListener(a -> {
            game.barSpeed = Math.max(1.0, game.barSpeed - 1.5);
            status.setText("Speed decreased: " + String.format("%.2f", game.barSpeed));
        });

        // Increase number of red slots (max 3)
        moreRed.addActionListener(a -> {
            if (game.redCount < 3) game.redCount++;
            status.setText("Red count: " + game.redCount);
            game.repaint();
        });

        // Decrease number of red slots (min 1)
        lessRed.addActionListener(a -> {
            if (game.redCount > 1) game.redCount--;
            status.setText("Red count: " + game.redCount);
            game.repaint();
        });

        frame.setLayout(new BorderLayout());
        frame.add(game, BorderLayout.CENTER);
        frame.add(topPanel, BorderLayout.NORTH);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // Main method: run GUI creation in event dispatch thread
    public static void main(String[] args) {
        SwingUtilities.invokeLater(ZonewallWTTG2::createAndShowGui);
    }
}
