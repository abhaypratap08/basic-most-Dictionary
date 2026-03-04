import java.awt.*;
import java.awt.event.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import javax.swing.*;
import javax.swing.border.*;

public class Dickt extends JFrame {
    private JTextField searchInput;
    private JLabel infoLabel, wordLabel;
    private JTextArea meaningArea, exampleArea;
    private JPanel resultContainer;

    public Dickt() {
        setUndecorated(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(350, 420);
        setAlwaysOnTop(true);
        getContentPane().setBackground(new Color(25, 25, 25));
        setLayout(new BorderLayout());

        JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.setBackground(new Color(40, 40, 40));
        searchPanel.setBorder(new EmptyBorder(10, 15, 10, 15));

        searchInput = new JTextField();
        searchInput.setFont(new Font("SansSerif", Font.PLAIN, 15));
        searchInput.setBackground(new Color(55, 55, 55));
        searchInput.setForeground(Color.WHITE);
        searchInput.setCaretColor(Color.WHITE);
        searchInput.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(70, 70, 70)), new EmptyBorder(5, 8, 5, 8)));
        
        searchInput.addActionListener(e -> handleSearch());
        searchPanel.add(searchInput, BorderLayout.CENTER);

        infoLabel = new JLabel("Search...", SwingConstants.CENTER);
        infoLabel.setForeground(Color.GRAY);
        infoLabel.setFont(new Font("SansSerif", Font.BOLD, 13));

        resultContainer = new JPanel();
        resultContainer.setLayout(new BoxLayout(resultContainer, BoxLayout.Y_AXIS));
        resultContainer.setBackground(new Color(25, 25, 25));
        resultContainer.setBorder(new EmptyBorder(10, 20, 20, 20));
        resultContainer.setVisible(false);

        wordLabel = new JLabel("WORD");
        wordLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        wordLabel.setForeground(new Color(100, 180, 255));

        meaningArea = createArea("Meaning");
        exampleArea = createArea("Example");

        resultContainer.add(wordLabel);
        resultContainer.add(Box.createRigidArea(new Dimension(0, 10)));
        resultContainer.add(meaningArea);
        resultContainer.add(Box.createRigidArea(new Dimension(0, 8)));
        resultContainer.add(exampleArea);

        add(searchPanel, BorderLayout.NORTH);
        add(infoLabel, BorderLayout.CENTER);
        add(resultContainer, BorderLayout.SOUTH);

        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) System.exit(0);
            }
        });

        setLocationRelativeTo(null);
        setVisible(true);    
    }
    
    private void handleSearch() {
        String word = searchInput.getText().trim();
        if (word.isEmpty()) return;
        infoLabel.setText("Fetching...");
        fetch(word);
    }

    private void fetch(String word) {
        String url = "https://api.dictionaryapi.dev/api/v2/entries/en/" + word;
        HttpClient.newHttpClient().sendAsync(
            HttpRequest.newBuilder().uri(URI.create(url)).build(), 
            HttpResponse.BodyHandlers.ofString()
        ).thenApply(HttpResponse::body).thenAccept(this::process);
    }

    private void process(String json) {
        SwingUtilities.invokeLater(() -> {
            if (json.contains("\"title\"")) {
                infoLabel.setText("Word Not Found");
                resultContainer.setVisible(false);
            } else {
                infoLabel.setText("");
                resultContainer.setVisible(true);
                wordLabel.setText(find(json, "\"word\":\"", "\"").toUpperCase());
                meaningArea.setText(find(json, "\"definition\":\"", "\""));
                String ex = find(json, "\"example\":\"", "\"");
                exampleArea.setText(ex.isEmpty() ? "No example provided." : ex);
                pack();
                setSize(350, getHeight());
            }
        });
    }

    private String find(String json, String key, String end) {
        int s = json.indexOf(key);
        if (s == -1) return "";
        s += key.length();
        return json.substring(s, json.indexOf(end, s));
    }

    private JTextArea createArea(String title) {
        JTextArea a = new JTextArea();
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setEditable(false);
        a.setBackground(new Color(25, 25, 25));
        a.setForeground(new Color(220, 220, 220));
        a.setBorder(BorderFactory.createTitledBorder(
            new LineBorder(new Color(60, 60, 60)), title, TitledBorder.LEFT, TitledBorder.TOP, null, Color.GRAY));
        return a;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Dickt::new);
    }
}
