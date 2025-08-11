import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;
import java.io.BufferedInputStream;
import javax.sound.sampled.*;

public class ZonewallWTTG2 extends JPanel implements ActionListener, MouseListener {
    static final int WIDTH = 900;
    static final int HEIGHT = 300;

    private final int MARGIN_LEFT = 60;
    private final int MARGIN_RIGHT = 60;
    private final int TRACK_Y = 120;
    private final int POINT_SIZE = 10;

    private final int SLOTS = 50;
    private int redCount = 3;
    private int redStart = 10;

    private double barX;
    private double barSpeed = 6.0;
    private final int BAR_PIXEL_WIDTH = 8;
    private boolean movingRight = true;

    private Timer timer;
    private boolean running = false;
    private Random rnd = new Random();
    private int tries = 0;
    private int successes = 0;
    private int consecutive = 0;

    private JLabel statusLabel;

    private String feedbackText = "";
    private Color feedbackColor = Color.WHITE;
    private long feedbackEndTime = 0;

    private int highlightSlotIndex = -1;
    private long highlightEndTime = 0;

    public ZonewallWTTG2() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        addMouseListener(this);
        timer = new Timer(16, this);
        resetPositions();
    }

    // Resets game variables and positions
    private void resetPositions() {
        barX = MARGIN_LEFT;
        movingRight = true;
        redStart = rnd.nextInt(SLOTS - redCount + 1);
        tries = 0;
        successes = 0;
        consecutive = 0;
        feedbackText = "";
        highlightSlotIndex = -1;
        highlightEndTime = 0;
    }

    private double slotCenterX(int slotIndex) {
        double trackWidth = WIDTH - MARGIN_LEFT - MARGIN_RIGHT;
        double slotWidth = trackWidth / (double) SLOTS;
        return MARGIN_LEFT + slotWidth * slotIndex + slotWidth / 2.0;
    }

    // Finds the slot closest to the bar's current position
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
        return Math.max(0, Math.min(bestIdx, SLOTS - 1));
    }


    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(Color.GREEN);
        g.setFont(new Font("SansSerif", Font.BOLD, 64));
        g.drawString("[", MARGIN_LEFT - 45, TRACK_Y + 20);
        g.drawString("]", (int)(slotCenterX(SLOTS - 1) + 20), TRACK_Y + 20);

        for (int i = 0; i < SLOTS; i++) {
            double cx = slotCenterX(i);
            int x = (int) Math.round(cx - POINT_SIZE / 2.0);
            int y = TRACK_Y;
            if (i == highlightSlotIndex && System.currentTimeMillis() < highlightEndTime) {
                g.setColor(Color.YELLOW);
            } else if (i >= redStart && i < redStart + redCount) {
                g.setColor(Color.RED);
            } else {
                g.setColor(new Color(0, 150, 0));
            }
            g.fillOval(x, y, POINT_SIZE, POINT_SIZE);
        }

        int bx = (int) Math.round(barX);
        g.setColor(new Color(180, 220, 255, 200));
        g.fillRect(bx, TRACK_Y - 25, BAR_PIXEL_WIDTH, 50);
        g.setColor(Color.CYAN);
        g.drawRect(bx, TRACK_Y - 25, BAR_PIXEL_WIDTH, 50);

        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.setColor(Color.WHITE);
        g.drawString("Tries: " + tries, 20, HEIGHT - 40);
        g.drawString("Successes: " + successes, 120, HEIGHT - 40);
        g.drawString("Consecutive: " + consecutive, 260, HEIGHT - 40);
        g.drawString(String.format("Speed: %.2f px/tick", barSpeed), 420, HEIGHT - 40);
        g.drawString("Red count: " + redCount, 620, HEIGHT - 40);

        if (!feedbackText.isEmpty() && System.currentTimeMillis() < feedbackEndTime) {
            g.setFont(new Font("SansSerif", Font.BOLD, 36));
            g.setColor(feedbackColor);
            FontMetrics fm = g.getFontMetrics();
            int textWidth = fm.stringWidth(feedbackText);
            g.drawString(feedbackText, (WIDTH - textWidth) / 2, HEIGHT / 4);
        }

        g.dispose();
    }

    // Updates bar position and reverses direction at edges
    @Override
    public void actionPerformed(ActionEvent e) {
        if (!running) return;
        if (movingRight) {
            barX += barSpeed;
            if (barX >= WIDTH - MARGIN_RIGHT - BAR_PIXEL_WIDTH) {
                barX = WIDTH - MARGIN_RIGHT - BAR_PIXEL_WIDTH;
                movingRight = false;
                playSound("/resources/soft_border.wav");
            }
        } else {
            barX -= barSpeed;
            if (barX <= MARGIN_LEFT) {
                barX = MARGIN_LEFT;
                movingRight = true;
                playSound("/resources/soft_border.wav");
            }
        }
        repaint();
    }

    // Handles mouse clicks and updates game state accordingly
    @Override
    public void mouseClicked(MouseEvent e) {
        if (!running) return;
        tries++;
        int idx = currentSlotIndex();

        highlightSlotIndex = idx;
        highlightEndTime = System.currentTimeMillis() + 500;

        if (idx >= redStart && idx < redStart + redCount) {
            successes++;
            consecutive++;
            playSound("/resources/success.wav");
            showFeedback("SUCCESS!", Color.GREEN);
            statusLabel.setText("SUCCESS! Slot " + idx + " red. Consecutive: " + consecutive);
            redStart = new Random().nextInt(SLOTS - redCount + 1);
        } else {
            consecutive = 0;
            playSound("/resources/fail.wav");
            showFeedback("FAIL!", Color.RED);
            statusLabel.setText("FAIL. Slot " + idx + " not red. Reset consecutive.");
        }
        repaint();
    }

    // Shows feedback message with color for a limited time
    private void showFeedback(String text, Color color) {
        feedbackText = text;
        feedbackColor = color;
        feedbackEndTime = System.currentTimeMillis() + 1000;
    }

    // Plays a sound from the specified resource path
    private void playSound(String resourcePath) {
        try {
            java.io.InputStream audioSrc = getClass().getResourceAsStream(resourcePath);
            if (audioSrc == null) {
                System.err.println("Sound not found: " + resourcePath);
                return;
            }
            java.io.InputStream bufferedIn = new BufferedInputStream(audioSrc);
            AudioInputStream ais = AudioSystem.getAudioInputStream(bufferedIn);
            Clip clip = AudioSystem.getClip();
            clip.open(ais);
            clip.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override public void mousePressed(MouseEvent e) {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}


    // Creates the game window lol
    private static void createAndShowGui() {
        JFrame frame = new JFrame("WTTG2 ZoneWall trainer");
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

        startBtn.addActionListener(a -> {
            if (!game.running) {
                game.running = true;
                game.timer.start();
                status.setText("Running. Click when bar is on red slot.");
            }
        });

        stopBtn.addActionListener(a -> {
            game.running = false;
            game.timer.stop();
            status.setText("Stopped.");
        });

        resetBtn.addActionListener(a -> {
            game.running = false;
            game.timer.stop();
            game.resetPositions();
            status.setText("Reset. New red start: " + game.redStart);
            game.repaint();
        });

        harderBtn.addActionListener(a -> {
            game.barSpeed = Math.min(24.0, game.barSpeed + 1.5);
            status.setText("Speed increased: " + String.format("%.2f", game.barSpeed));
        });

        easierBtn.addActionListener(a -> {
            game.barSpeed = Math.max(1.0, game.barSpeed - 1.5);
            status.setText("Speed decreased: " + String.format("%.2f", game.barSpeed));
        });

        moreRed.addActionListener(a -> {
            if (game.redCount < 3) game.redCount++;
            status.setText("Red count: " + game.redCount);
            game.repaint();
        });

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

    // Launches the application GUI
    public static void main(String[] args) {
        SwingUtilities.invokeLater(ZonewallWTTG2::createAndShowGui);
    }
}
