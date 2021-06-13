import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This class represents a GUI for this program.
 *
 * @author Seyyed Mohammad Hejazi Hoseini
 */
public class GUI
{
    ExecutorService executor = Executors.newCachedThreadPool();

    //Main frame of the program.
    JFrame frame;
    //The frame position, it is used for saving the position of the frame after
    //we go to fullscreen mode
    Rectangle framePosition;
    //The extendedState of the program
    int extendedState;
    //The options in the options Frame
    boolean[] options;

    //Main panels of the program
    HistoryPanel leftPanel;
    JPanel midPanel;
    JPanel rightPanel;
    JPanel midTopPanel;

    //Main Components of the request
    JTextArea urlTextArea;
    JComboBox<String> methodBox;
    JComboBox<String> massageBodyTypeBox;
    KeyValuePanel requestHeaders;
    KeyValuePanel responseHeaders;
    JTabbedPane massageBody;
    KeyValuePanel formData;
    JTextArea responseBody;
    PreviewPanel preview;

    //The currently sent response
    Request request = null;


    /**
     * Create a new GUI for the program and make it visible.
     */
    public GUI()
    {
        options = new boolean[]{false, false};

        readOptions();

        //Create the main frame
        frame = new JFrame("PostPost");
        frame.addWindowListener(new WindowClosingListener());

        //Create the mainPanel, it holds all the sub panels.
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));

        //Just a Dimension for later in panel's sizes
        Dimension panelSize = new Dimension(500, 500);

        //Create the left Panel
        leftPanel = new HistoryPanel();
        leftPanel.setPreferredSize(new Dimension(200, 500));
        leftPanel.setAlignmentY(Component.TOP_ALIGNMENT);
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.reloadRequests();

        //Create the middle Panel
        midPanel = new JPanel();
        {
            midPanel.setLayout(new BorderLayout());
            midPanel.setAlignmentY(Component.TOP_ALIGNMENT);
            midPanel.setPreferredSize(panelSize);
            midPanel.setMinimumSize(panelSize);
            {
                JPanel midTopPanel = new JPanel();
                midTopPanel.setLayout(new BorderLayout());
                midTopPanel.setBorder(BorderFactory.createEmptyBorder
                        (5, 5, 0, 0));

                JButton sendButton = new JButton("Send");
                sendButton.addActionListener(new sendButtonListener());
                JTextArea urlTextArea = new JTextArea(1, 30);
                urlTextArea.setAlignmentY(JTextArea.CENTER_ALIGNMENT);
                JScrollPane urlTextAreScroll = new JScrollPane(urlTextArea,
                        JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
                urlTextAreScroll.setPreferredSize(new Dimension(500, 4));
                urlTextArea.setFont(urlTextArea.getFont().deriveFont(15f));

                JComboBox<String> methodBox = new JComboBox<>(new String[]{
                        "GET", "POST", "PUT", "DELETE"});
                methodBox.setFocusable(false);

                midTopPanel.add(methodBox, BorderLayout.WEST);
                midTopPanel.add(urlTextAreScroll, BorderLayout.CENTER);
                midTopPanel.add(sendButton, BorderLayout.EAST);

                JTabbedPane midTab = new JTabbedPane();
                JComboBox<String> massageBodyTypeBox = new JComboBox<>(new String[]{
                        "No Body", "Form Data"});
                massageBodyTypeBox.setFocusable(false);

                JPanel noBody = new JPanel();
                JScrollPane noBodyScroll = new JScrollPane(noBody,
                        JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

                KeyValuePanel formData = new KeyValuePanel(1);
                JScrollPane formDataScroll = new JScrollPane(formData,
                        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                        JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);


                massageBodyTypeBox.addActionListener(new JComboBoxListListener(
                        midTab, massageBodyTypeBox, noBodyScroll, formDataScroll));

                KeyValuePanel requestHeaders = new KeyValuePanel(1);
                JScrollPane requestHeadersScroll = new JScrollPane(requestHeaders,
                        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                        JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

                midTab.add("Body", noBodyScroll);
                midTab.add("Header", requestHeadersScroll);

                midTab.setTabComponentAt(0, massageBodyTypeBox);

                midPanel.add(midTopPanel, BorderLayout.NORTH);
                midPanel.add(midTab, BorderLayout.CENTER);

                this.urlTextArea = urlTextArea;
                this.methodBox = methodBox;
                this.massageBodyTypeBox = massageBodyTypeBox;
                this.requestHeaders = requestHeaders;
                this.massageBody = midTab;
                this.formData = formData;
            }
        }

        //Create the right Panel
        rightPanel = new JPanel();
        {
            rightPanel.setLayout(new BorderLayout());
            rightPanel.setAlignmentY(Component.TOP_ALIGNMENT);
            rightPanel.setPreferredSize(panelSize);
            {
                JPanel midTopPanel = new JPanel();
                midTopPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

                JTextField statusMassage = new JTextField("      ");
                statusMassage.setEditable(false);
                statusMassage.setOpaque(true);
                statusMassage.setBackground(Color.GREEN);
                JTextField responseTime = new JTextField("     ");
                responseTime.setEditable(false);
                JTextField responseSize = new JTextField("       ");
                responseSize.setEditable(false);

                midTopPanel.add(statusMassage);
                midTopPanel.add(responseTime);
                midTopPanel.add(responseSize);

                JTabbedPane midBotTab = new JTabbedPane();

                JComboBox<String> bodyList = new JComboBox<>(new String[]{
                        "Raw", "Preview"});
                bodyList.setFocusable(false);

                JPanel raw = new JPanel();
                raw.setLayout(new BorderLayout());
                JScrollPane rawScroll = new JScrollPane(raw,
                        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

                {
                    JTextArea responseBody = new JTextArea();
                    responseBody.setEditable(false);
                    responseBody.setWrapStyleWord(true);
                    responseBody.setLineWrap(true);
                    responseBody.setMargin( new Insets(0,10,20,20));
                    raw.add(responseBody, BorderLayout.CENTER);
                    this.responseBody = responseBody;
                }

                PreviewPanel preview = new PreviewPanel();
                JScrollPane previewScroll = new JScrollPane(preview,
                        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                        JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

                KeyValuePanel responseHeaders = new KeyValuePanel(0);
                JScrollPane responseHeadersScroll = new JScrollPane(responseHeaders,
                        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                        JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

                bodyList.addActionListener(new JComboBoxListListener(
                        midBotTab, bodyList, rawScroll, previewScroll));

                midBotTab.add("Raw", rawScroll);
                midBotTab.add("Header", responseHeadersScroll);

                midBotTab.setTabComponentAt(0, bodyList);

                rightPanel.add(midTopPanel, BorderLayout.NORTH);
                rightPanel.add(midBotTab, BorderLayout.CENTER);

                this.responseHeaders = responseHeaders;
                this.preview = preview;
                this.midTopPanel = midTopPanel;
            }
        }

        //Add all the panels to the main panel
        mainPanel.add(leftPanel);
        mainPanel.add(midPanel);
        mainPanel.add(rightPanel);

        //Create the menu for the main frame
        JMenuBar menuBar = new JMenuBar();
        {
            JMenu applicationMenu = new JMenu("Application");
            applicationMenu.setMnemonic(KeyEvent.VK_A);

            JMenuItem saveItem = new JMenuItem("Save Request");
            saveItem.setMnemonic(KeyEvent.VK_S);
            saveItem.setAccelerator(KeyStroke.getKeyStroke("control S"));
            saveItem.addActionListener(new SaveItemListener());

            JMenuItem outputItem = new JMenuItem("Save Massage Body");
            outputItem.setMnemonic(KeyEvent.VK_S);
            outputItem.setAccelerator(KeyStroke.getKeyStroke("control O"));
            outputItem.addActionListener(new OutputItemListener());

            JMenuItem optionsItem = new JMenuItem("Options");
            optionsItem.setMnemonic(KeyEvent.VK_O);
            optionsItem.setAccelerator(KeyStroke.getKeyStroke("control P"));
            optionsItem.addActionListener(new OptionsItemListener());

            JMenuItem exitItem = new JMenuItem("Exit");
            exitItem.setMnemonic(KeyEvent.VK_E);
            exitItem.setAccelerator(KeyStroke.getKeyStroke("control E"));
            exitItem.addActionListener(new ExitItemListener());

            applicationMenu.add(saveItem);
            applicationMenu.add(outputItem);
            applicationMenu.add(optionsItem);
            applicationMenu.add(exitItem);

            JMenu viewMenu = new JMenu("View");
            viewMenu.setMnemonic(KeyEvent.VK_V);

            JMenuItem toggleFullScreenItem = new JMenuItem(
                    "Toggle FullScreen");
            toggleFullScreenItem.setMnemonic(KeyEvent.VK_T);
            toggleFullScreenItem.setAccelerator(
                    KeyStroke.getKeyStroke("control F"));
            toggleFullScreenItem.addActionListener(
                    new ToggleFullScreenItemListener());

            JMenuItem toggleSideBarItem = new JMenuItem("Toggle Side Bar");
            toggleSideBarItem.setMnemonic(KeyEvent.VK_T);
            toggleSideBarItem.setAccelerator(
                    KeyStroke.getKeyStroke("control A"));
            toggleSideBarItem.addActionListener(new ToggleSideBarItemListener());
            viewMenu.add(toggleFullScreenItem);
            viewMenu.add(toggleSideBarItem);

            JMenu helpMenu = new JMenu("Help");
            helpMenu.setMnemonic(KeyEvent.VK_H);

            JMenuItem helpItem = new JMenuItem("Help");
            helpItem.setMnemonic(KeyEvent.VK_H);
            helpItem.setAccelerator(KeyStroke.getKeyStroke("control K"));
            helpItem.addActionListener(new HelpItemListener());

            JMenuItem aboutItem = new JMenuItem("About");
            aboutItem.setMnemonic(KeyEvent.VK_A);
            aboutItem.setAccelerator(KeyStroke.getKeyStroke("control M"));
            aboutItem.addActionListener(new AboutItemListener());

            helpMenu.add(helpItem);
            helpMenu.add(aboutItem);

            menuBar.add(applicationMenu);
            menuBar.add(viewMenu);
            menuBar.add(helpMenu);
        }

        //Add everything to the main frame
        frame.setJMenuBar(menuBar);
        frame.add(mainPanel);

        frame.setBounds(20, 20, 1200, 600);
        frame.setVisible(true);
    }

    /**
     * This is the main method for running the GUI.
     * @param args unused
     */
    public static void main(String[] args)
    {
        new GUI();
    }

    /**
     * This class represents the KeyValue panel in both mid and right panel. It
     * is used for formData request headers and response headers.
     */
    private class KeyValuePanel extends JPanel
    {
        Dimension preferredSize;
        Dimension maximumSize;
        int mode;

        public KeyValuePanel(int mode)
        {
            this.setAlignmentY(Component.TOP_ALIGNMENT);
            this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            this.mode = mode;

            preferredSize = new Dimension(150, 50);
            maximumSize = new Dimension(350, 50);

            JPanel newEntry = new JPanel();
            if (mode == 1)
            {
                newEntry.setLayout(new BoxLayout(newEntry, BoxLayout.X_AXIS));

                JTextField newHeader = new JTextField("Key");
                newHeader.setPreferredSize(preferredSize);
                newHeader.setMaximumSize(maximumSize);
                newHeader.setMinimumSize(preferredSize);
                newHeader.setSize(preferredSize);
                newHeader.addMouseListener(new NewHeader());
                newEntry.add(newHeader);

                JTextField newValue = new JTextField("Value");
                newValue.setPreferredSize(preferredSize);
                newValue.setMaximumSize(maximumSize);
                newValue.setSize(preferredSize);
                newValue.setMinimumSize(preferredSize);
                newValue.addMouseListener(new NewHeader());
                newEntry.add(newValue);


                JButton delete = new JButton();
                delete.setOpaque(false);
                delete.setContentAreaFilled(false);
                delete.setBorderPainted(false);
                delete.setPreferredSize(new Dimension(64, 26));
                delete.setMaximumSize(new Dimension(64, 26));
                delete.setMinimumSize(new Dimension(64, 26));
                newEntry.add(delete);

                this.add(newEntry);
            }

            if (mode == 0)
            {
                JButton copy = new JButton("Copy to Clipboard");
                copy.addActionListener(new CopyToClipBoardListener());
                copy.setAlignmentX(JButton.CENTER_ALIGNMENT);
                this.add(Box.createRigidArea(new Dimension(5, 20)));
                this.add(copy);
            }
        }

        private void addKeyValue(JTextField key, JTextField value)
        {
            JPanel newEntry = new JPanel();
            newEntry.setLayout(new BoxLayout(newEntry, BoxLayout.X_AXIS));

            if (mode == 0)
            {
                key.setEditable(false);
                value.setEditable(false);
            }

            key.setPreferredSize(preferredSize);
            key.setMaximumSize(maximumSize);
            key.setSize(preferredSize);
            key.setMinimumSize(preferredSize);
            newEntry.add(key);

            value.setPreferredSize(preferredSize);
            value.setMaximumSize(maximumSize);
            value.setSize(preferredSize);
            value.setMinimumSize(preferredSize);
            newEntry.add(value);

            if (mode == 1)
            {
                JButton delete = new JButton("X");
                delete.addActionListener(
                        new DeleteButton(this, newEntry));
                newEntry.add(delete);

                JCheckBox checkBox = new JCheckBox();
                checkBox.setSelected(true);
                newEntry.add(checkBox);
            }

            if (mode == 0)
                this.add(newEntry, this.getComponentCount() - 2);
            else
                this.add(newEntry, this.getComponentCount() - 1);

            this.revalidate();
        }

        private void resetKeyValues(int mode)
        {
            Component[] components = this.getComponents();
            for (int i = 0; i < components.length - mode; i++)
            {
                if (components[i] instanceof JPanel)
                {
                    this.remove((JPanel) components[i]);
                }
            }
            this.repaint();
            this.revalidate();
        }

        public String setKeyValues(Map<String, List<String>> keyValues, int mode)
        {
            this.resetKeyValues(mode);

            if (keyValues == null)
                return "";

            String contentType = "";
            for (Map.Entry<String, List<String>> entry : keyValues.entrySet())
            {
                String k = entry.getKey();
                String v = entry.getValue().toString();

                if (k == null)
                    continue;

                if (k.equals("Content-Type"))
                    contentType = v;

                JTextField key = new JTextField(k);
                JTextField value = new JTextField(v.substring(1, v.length() - 1));
                this.addKeyValue(key, value);
            }
            return contentType;
        }

        private class NewHeader extends MouseAdapter
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                super.mousePressed(e);
                JTextField header = new JTextField("Key");
                JTextField value = new JTextField("Value");
                addKeyValue(header, value);
                if (((JTextField) e.getComponent()).getText().equals("New header"))
                    header.requestFocus();
                else
                    value.requestFocus();
            }
        }

        private class DeleteButton implements ActionListener
        {
            JPanel newEntry;
            JPanel header;

            public DeleteButton(JPanel header, JPanel newEntry)
            {
                this.newEntry = newEntry;
                this.header = header;
            }

            @Override
            public void actionPerformed(ActionEvent e)
            {
                header.remove(newEntry);
                header.revalidate();
            }

        }
    }

    /**
     * This class represents the History panel in the left.
     */
    private class HistoryPanel extends JPanel
    {

        public HistoryPanel()
        {
            this.setAlignmentY(Component.TOP_ALIGNMENT);
            this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        }

        public void addRequest(Request request)
        {
            String name = request.getMethod() + " " + request.getDate();
            JTextField requestField = new JTextField(name);
            requestField.setEditable(false);
            requestField.setHorizontalAlignment(JTextField.CENTER);
            requestField.setOpaque(true);
            requestField.setForeground(Color.WHITE);
            requestField.setMaximumSize(new Dimension(240, 50));
            requestField.setPreferredSize(new Dimension(200, 50));
            requestField.setBackground(Color.GRAY);
            requestField.addMouseListener(new RequestFieldListener(leftPanel));

            this.add(requestField);
            this.revalidate();
            this.repaint();
        }

        private void saveRequest(Request request)
        {
            request.saveRequest(request.getDate());
            for (Component c : this.getComponents())
            {
                JTextField field = (JTextField) c;
                if (field.getText().equals(request.getMethod() + " " + request.getDate()))
                {
                    return;
                }
            }
            this.addRequest(request);
        }

        private void reloadRequests()
        {
            File requestHistoryFile = new File("./RequestHistory/");
            String[] requestNames = requestHistoryFile.list();
            if (requestNames == null)
                return;

            for (int i = 0; i < requestNames.length; i++)
            {
                Request loadedRequest = Console.loadRequest(i, 0);
                this.addRequest(loadedRequest);
            }
        }

        private class RequestFieldListener extends MouseAdapter
        {
            JPanel container;

            public RequestFieldListener(JPanel container)
            {
                this.container = container;
            }

            @Override
            public void mousePressed(MouseEvent e)
            {
                JTextField requestField = (JTextField) e.getSource();
                int count = -1;
                for (int i = 0; i < container.getComponentCount(); i++)
                {
                    JTextField field = (JTextField) container.getComponent(i);
                    if (field.equals(requestField))
                    {
                        count = i;
                    }
                }

                Request requestToLoad = Console.loadRequest(count, 0);

                urlTextArea.setText(requestToLoad.getUrl().toString());
                methodBox.setSelectedItem(requestToLoad.getMethod());
                formData.setKeyValues(requestToLoad.getFormData(), 1);
                requestHeaders.setKeyValues(requestToLoad.getRequestHeaders(), 1);
                responseHeaders.setKeyValues(requestToLoad.getResponseHeaders(), 2);
                responseBody.setText(new String(requestToLoad.getResponseBody()));
                preview.setImageLabel(requestToLoad.getResponseBody());
                ((JTextField) midTopPanel.getComponent(0)).setText(requestToLoad.getResponseStatus());
                ((JTextField) midTopPanel.getComponent(1)).setText(requestToLoad.getResponseTime() + "ms");
                ((JTextField) midTopPanel.getComponent(2)).setText(requestToLoad.getResponseSize());
                midTopPanel.revalidate();

            }
        }
    }

    /**
     * This class represents a handler that is runnable that links the Console
     * and GUI together.
     */
    private class Handler implements Runnable
    {
        String[] args;

        public Handler(String[] args)
        {
            this.args = args;
        }

        @Override
        public void run()
        {
            Console console = new Console(args);
            Request consoleRequest = console.getRequest();
            request = consoleRequest;

            if (request == null)
            {
                JOptionPane.showMessageDialog(new Frame(),
                        "Error: Request did not work",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Map<String, List<String>> headers = consoleRequest.getResponseHeaders();

            String contentType;
            contentType = responseHeaders.setKeyValues(headers,2);
            responseHeaders.revalidate();

            byte[] responseBytes = consoleRequest.getResponseBody();
            if (responseBytes != null)
                responseBody.setText(new String(responseBytes));
            responseBody.revalidate();

            if (request.getResponseStatus() != null)
                ((JTextField) midTopPanel.getComponent(0)).
                        setText(request.getResponseStatus());
            if (request.getResponseTime() != null)
                ((JTextField) midTopPanel.getComponent(1)).
                        setText(String.valueOf(request.getResponseTime()) + "ms");
            if (request.getResponseSize() != null)
                ((JTextField) midTopPanel.getComponent(2)).
                        setText(request.getResponseSize());
            midTopPanel.revalidate();
            midTopPanel.repaint();

            if (contentType.contains("image"))
                preview.setImageLabel(responseBytes);
            else
                preview.clear();

            preview.revalidate();
            preview.repaint();
        }
    }

    /**
     * This class represents the preview panel in the right. It is used when
     * massage body is a picture.
     */
    private class PreviewPanel extends JPanel
    {
        public PreviewPanel()
        {
            this.setLayout(new BorderLayout());

        }

        public void setImageLabel(byte[] responseBytes)
        {
            clear();
            try
            {
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(responseBytes));
                JLabel imageLabel = new JLabel(new ImageIcon(image));
                this.add(imageLabel, BorderLayout.CENTER);
                this.revalidate();
                this.repaint();
            }
            catch (IOException | NullPointerException ignored){}
        }

        public void clear()
        {
            if (this.getComponentCount() != 0)
                this.remove(0);
            this.revalidate();
            this.repaint();
        }
    }

    /**
     * This class represents an action listener for the send button. It mainly
     * makes the String[] args to pass to the Handler and then executed by the
     * executor service.
     */
    private class sendButtonListener implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            ArrayList<String> args = new ArrayList<>();

            args.add(urlTextArea.getText());

            args.add("-M");
            args.add((String) methodBox.getSelectedItem());
            args.add("-i");

            Component[] headerPanels = requestHeaders.getComponents();
            for (int i = 0; i < headerPanels.length - 1; i++)
            {
                JPanel headerPanel = (JPanel) headerPanels[i];

                JCheckBox check = (JCheckBox) headerPanel.getComponent(3);
                if (!check.isSelected())
                   continue;

                JTextField key = (JTextField) headerPanel.getComponent(0);
                JTextField value = (JTextField) headerPanel.getComponent(1);

                String header = new String(key.getText() + ":" + value.getText());

                args.add("-H");
                args.add(header);
            }

            if (massageBodyTypeBox.getSelectedIndex() == 1)
            {
                KeyValuePanel formData = (KeyValuePanel)
                        ((JScrollPane) massageBody.getComponentAt(0)).getViewport().getView();
                Component[] keyValues = formData.getComponents();
                for (int i = 0; i < keyValues.length - 1; i++)
                {
                    JPanel keyValuePanel = (JPanel) keyValues[i];

                    JCheckBox check = (JCheckBox) keyValuePanel.getComponent(3);
                    if (!check.isSelected())
                        continue;

                    JTextField key = (JTextField) keyValuePanel.getComponent(0);
                    JTextField value = (JTextField) keyValuePanel.getComponent(1);

                    String keyValue = new String(key.getText() + "=" + value.getText());

                    args.add("-d");
                    args.add(keyValue);
                }
            }

            String[] argsString = args.toArray(new String[0]);

            executor.execute(new Handler(argsString));

        }
    }

    /**
     * This class represents an action listener for the copy button. It copies
     * all the response headers to the clipboard.
     */
    private class CopyToClipBoardListener implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            String headers = request.getResponseHeaders().toString();
            StringSelection selection = new StringSelection(headers);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, selection);
        }
    }

    /**
     * This class represents an action listener for the save item in application
     * menu. It saves the sent request and can be reloaded later.
     */
    private class SaveItemListener implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            if (request != null)
                leftPanel.saveRequest(request);
        }
    }

    /**
     * This class represents an action listener for the output item in
     * application menu. It writes the response body of the request to a file
     * in ResponseOutput directory.
     */
    private class OutputItemListener implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            if (request != null)
                request.saveResponseBody(null);
        }
    }

    /**
     * This class represents an actionListener for the Exit menu item.
     */
    private class ExitItemListener implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            System.exit(0);
        }
    }

    /**
     * This class represents an actionListener for the Option menu item.
     */
    private class OptionsItemListener implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            JFrame optionFrame = new JFrame("Options");
            optionFrame.setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();

            JCheckBox followRedirect = new JCheckBox("Follow Redirect");
            if (options[0])
                followRedirect.setSelected(true);
            JCheckBox systemTray = new JCheckBox("System Tray");
            if (options[1])
                systemTray.setSelected(true);

            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridx = 0;
            gbc.gridy = 0;
            optionFrame.add(followRedirect, gbc);
            gbc.gridy = 1;
            optionFrame.add(systemTray, gbc);

            optionFrame.addWindowListener(new WindowAdapter()
            {
                @Override
                public void windowClosing(WindowEvent e)
                {
                    options[0] = followRedirect.isSelected();
                    options[1] = systemTray.isSelected();

                }
            });

            optionFrame.setLocationRelativeTo(frame);
            optionFrame.setSize(200, 100);
            optionFrame.setResizable(false);
            optionFrame.setVisible(true);
        }
    }

    /**
     * This class represents an actionListener for the ToggleFullScreen menu
     * item.
     */
    private class ToggleFullScreenItemListener implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent e)
        {

            if (frame.isUndecorated())
            {
                frame.dispose();
                frame.setExtendedState(extendedState);
                frame.setBounds(framePosition);
                frame.setUndecorated(false);
                frame.setVisible(true);
            }
            else
            {
                framePosition = frame.getBounds();
                extendedState = frame.getExtendedState();
                frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
                frame.dispose();
                frame.setUndecorated(true);
                frame.setVisible(true);
            }
        }
    }

    /**
     * This class represents an actionListener for the ToggleSideBar menu item.
     */
    private class ToggleSideBarItemListener implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            if (leftPanel.isVisible())
                leftPanel.setVisible(false);
            else
                leftPanel.setVisible(true);
        }
    }

    /**
     * This class represents an actionListener for the Help menu item.
     */
    private class HelpItemListener implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            JFrame help = new JFrame("Help");

            JTextArea helpText = new JTextArea(
                    "Write URL in the text field and specify your method, " +
                    "body and headers; then press send.\nYou can view the" +
                    "the response in right panel.\nYou can save the output" +
                    "and the request in the the application menu or using" +
                    "their accelerators.\nYou can only save after you send the request\n"+
                    "To reload a saved request, simply" +
                    "click on the desired request in the left panel.\n" +
                    "If you want to upload a file, in formData tab, write" +
                    "file in key and path of the file in value.\n");
            helpText.setFont(urlTextArea.getFont().deriveFont(16f));
            helpText.setLineWrap(true);
            helpText.setWrapStyleWord(true);
            helpText.setEditable(false);
            helpText.setFocusable(false);

            help.add(helpText);

            help.setLocationRelativeTo(frame);
            help.setSize(600, 300);
            help.setVisible(true);
        }
    }

    /**
     * This class represents an actionListener for the About menu item.
     */
    private class AboutItemListener implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            JFrame about = new JFrame();
            about.setResizable(false);
            about.setFocusable(false );
            about.setLayout(new BoxLayout(about.getContentPane(),
                    BoxLayout.Y_AXIS));

            JTextField field1 = new JTextField("Seyyed Mohammad Hejazi Hoseini");
            field1.setEditable(false);
            field1.setHorizontalAlignment(JTextField.CENTER);
            field1.setFocusable(false);
            JTextField field2 = new JTextField("9733020");
            field2.setEditable(false);
            field2.setHorizontalAlignment(JTextField.CENTER);
            field2.setFocusable(false);

            about.add(field1);
            about.add(field2);

            about.setLocationRelativeTo(frame);
            about.setSize(new Dimension(250, 100));
            about.setVisible(true);
        }
    }

    /**
     * This class represents an actionListener for the JComboBox in the mid and
     * right tab. It takes action when you click it.
     */
    private class JComboBoxListListener implements ActionListener
    {
        JTabbedPane midTab;
        JComboBox<String> box;
        JScrollPane[] scrollPanes;

        public JComboBoxListListener(JTabbedPane midTab, JComboBox<String> box,
                                      JScrollPane... scrollPanes)
        {
            this.midTab = midTab;
            this.box = box;
            this.scrollPanes = scrollPanes;

            MouseListener listener = new MouseAdapter()
            {
                @Override
                public void mouseClicked(MouseEvent e)
                {
                    super.mouseClicked(e);
                    midTab.setSelectedIndex(0);

                }
            };

            box.getComponent(0).addMouseListener(listener);
            box.addMouseListener(listener);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            int selectedTab = box.getSelectedIndex();

            midTab.setComponentAt(0, scrollPanes[selectedTab]);
            midTab.revalidate();
            midTab.repaint();
        }
    }

    /**
     * This class represents an actionListener for when the main frame closes.
     * It saves the options in file called "options".
     */
    private class WindowClosingListener extends WindowAdapter
    {

        @Override
        public void windowClosing(WindowEvent e)
        {
            if (!options[1])
            {
                saveOptions();
                System.exit(0);
            }

            super.windowClosing(e);
            SystemTray tray = SystemTray.getSystemTray();

            frame.setExtendedState(JFrame.ICONIFIED);

            PopupMenu popup = new PopupMenu();
            MenuItem defaultItem = new MenuItem("Exit");
            defaultItem.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    saveOptions();
                    System.exit(0);
                }
            });

            popup.add(defaultItem);

            BufferedImage image = new BufferedImage(
                    100, 100, BufferedImage.TYPE_INT_RGB);

            TrayIcon trayIcon = new TrayIcon(image,
                    "PostPost", popup);

            trayIcon.addMouseListener(new MouseAdapter()
            {
                @Override
                public void mouseClicked(MouseEvent e)
                {
                    if (e.getClickCount() >= 2)
                        java.awt.EventQueue.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                frame.setVisible(true);
                                frame.setExtendedState(JFrame.NORMAL);
                                tray.remove(trayIcon);
                            }
                        });
                }
            });

            try
            {
                tray.add(trayIcon);
            } catch (AWTException ex)
            {
                ex.printStackTrace();
            }
        }
     }

    /**
     * This method saves the options into a file called "Options".
     */
    private void saveOptions()
     {
         try
         {
             FileWriter fileWriter = new FileWriter("options");
             for (Boolean bool : options)
             {
                 if (bool)
                     fileWriter.write("1");
                 else
                     fileWriter.write("0");
             }
             fileWriter.close();
         }
        catch (IOException e)
        {
            System.out.println("An error occurred.");
            System.exit(1);
        }

     }

    /**
     * This method reads the options from the file "Options".
     */
    private void readOptions()
     {
         FileReader fileReader;
         try
         {
             fileReader = new FileReader("options");
         }
         catch (IOException e)
         {
             return;
         }

         for (int i = 0; i < options.length; i++)
         {
             try
             {
                 int c =  fileReader.read();
                 if (c == (int) '0')
                 {
                     options[i] = false;
                 }
                 else
                 {
                     options[i] = true;
                 }
             }
             catch (IOException e)
             {
                 return;
             }

         }
     }
}