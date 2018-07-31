package com.apw.carcontrol;

import com.apw.ImageManagement.ImageManagementModule;
import com.apw.SpeedCon.SpeedControlModule;
import com.apw.Steering.SteeringModule;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MrModule extends JFrame implements Runnable, KeyListener {
    private ScheduledExecutorService executorService;
    private BufferedImage displayImage, bufferImage;
    private GraphicsDevice graphicsDevice;
    private ArrayList<Module> modules;
    private ImageIcon displayIcon;
    private CarControl control;
    private boolean fullscreen;
    private JPanel panel;

    // FIXME breaks if dimensions are not 912x480
    private final int windowWidth = 912;
    private final int windowHeight = 480;

    private MrModule(boolean renderWindow) {
        if (renderWindow) {
            control = new TrakSimControl();
            headlessInit();
            setupWindow();
        } else {
            control = null;
            headlessInit();
        }

        createModules();
    }

    private void headlessInit() {
        executorService = Executors.newSingleThreadScheduledExecutor();
        modules = new ArrayList<>();
        executorService.scheduleAtFixedRate(this, 0, 1000 / 1000, TimeUnit.MILLISECONDS);
    }

    private void setupWindow() {
        graphicsDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        displayImage = new BufferedImage(windowWidth, windowHeight, BufferedImage.TYPE_INT_RGB);
        bufferImage = new BufferedImage(windowWidth, windowHeight, BufferedImage.TYPE_INT_RGB);
        displayIcon = new ImageIcon(displayImage);
        panel = new JPanel();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(windowWidth, windowHeight + 25);
        setResizable(true);
        setVisible(true);
        addKeyListener(this);
        add(panel);
    }

    private void createModules() {
        modules.add(new ImageManagementModule(windowWidth, windowHeight));
        modules.add(new SpeedControlModule());
        modules.add(new SteeringModule());

        for (Module module : modules)
            module.initialize(control);
    }

    private void update() {
        if (control instanceof TrakSimControl) {
            ((TrakSimControl) control).cam.theSim.SimStep(1);
        }

        control.readCameraImage();
        control.setEdges(getInsets());
        for (Module module : modules) {
            module.update(control);
        }
    }

    @Override
    public void paint(Graphics g) {
        if (!(control instanceof TrakSimControl)) {
            return;
        }

        super.paint(g);

        g.drawImage(displayImage, 0, 0, getWidth(), getHeight(), null);

        int[] renderedImage = ((TrakSimControl) control).getRenderedImage();

        if (renderedImage != null) {
            int[] displayPixels = ((DataBufferInt) bufferImage.getRaster().getDataBuffer()).getData();
            System.arraycopy(renderedImage, 0, displayPixels, 0, renderedImage.length);

            BufferedImage tempImage = displayImage;
            displayImage = bufferImage;
            bufferImage = tempImage;

            displayIcon.setImage(displayImage);
        }

        for (Module module : modules) {
            module.paint(control, g);
        }
    }

    @Override
    public void run() {
        update();
        repaint();
    }

    public static void main(String[] args) {
        boolean renderWindow = true;
        if(args.length > 0 && args[0].toLowerCase().equals("nosim")) {
            renderWindow = false;
        }
        new MrModule(renderWindow);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (!(control instanceof TrakSimControl)) {
            return;
        }

        if (e.getKeyCode() == KeyEvent.VK_F) {
            fullscreen = !fullscreen;
            setVisible(false);
            dispose();
            setUndecorated(fullscreen);
            if (fullscreen) {
                graphicsDevice.setFullScreenWindow(this);
                validate();
            } else {
                graphicsDevice.setFullScreenWindow(null);
                setVisible(true);
            }
        }

        for (Map.Entry<Integer, Runnable> binding : ((TrakSimControl) control).keyBindings.entrySet()) {
            if (e.getKeyCode() == binding.getKey()) {
                binding.getValue().run();
            }
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {  }

    @Override
    public void keyReleased(KeyEvent e) {  }
}
