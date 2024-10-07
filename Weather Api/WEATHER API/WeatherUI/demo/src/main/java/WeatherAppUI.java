import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class WeatherAppUI extends JFrame {

    private static final String WEATHER_API_KEY = "3d29ec11647646df94e93355240410";  // Replace with your actual weather API key
    private static final String WEATHER_BASE_URL = "https://api.weatherapi.com/v1/current.json";

    // UI components
    private JLabel weatherIconLabel;
    private JLabel temperatureLabel;
    private JLabel conditionLabel;
    private JLabel humidityLabel;
    private JLabel windSpeedLabel;
    private JLabel dateTimeLabel;
    private JTextField cityInput;

    public WeatherAppUI() {
        // Set up the UI
        System.out.println("UI Setup started...");  // Debugging log
        setTitle("Weather Today");
        setSize(400, 700);  // Increased height
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);  // Center the window
        setLayout(new GridBagLayout()); // Use GridBagLayout for centering components

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10);  // Set padding around components

        // Create search bar panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        cityInput = new JTextField(20);  // Make it wider
        cityInput.setFont(new Font("Arial", Font.PLAIN, 14));
        cityInput.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                    fetchWeatherData();
                }
            }
        });
        searchPanel.add(cityInput);

        // Add search panel to the top (centered)
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        add(searchPanel, gbc);

        // Create weather info panel
        JPanel weatherPanel = new RoundedPanel(40, new Color(224, 242, 254));
        weatherPanel.setLayout(new BoxLayout(weatherPanel, BoxLayout.Y_AXIS));  // Stack components vertically
        weatherPanel.setPreferredSize(new Dimension(250, 400));  // Increased height to fit content
        weatherPanel.setMaximumSize(new Dimension(250, 400));  // Prevent resizing
        
        // Date and time label
        dateTimeLabel = new JLabel("October 11, 2024 | 8:16 AM", SwingConstants.CENTER);
        dateTimeLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        dateTimeLabel.setForeground(new Color(128, 128, 128));  // Gray font

        // Weather icon
        weatherIconLabel = new JLabel(new ImageIcon(getClass().getResource("/com/example/images/default.png")), SwingConstants.CENTER);  // Default placeholder icon
        weatherIconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Temperature label
        temperatureLabel = new JLabel("25.3°C", SwingConstants.CENTER);
        temperatureLabel.setFont(new Font("Arial", Font.BOLD, 40));
        temperatureLabel.setForeground(new Color(51, 153, 255));  // Light blue font
        temperatureLabel.setAlignmentX(Component.CENTER_ALIGNMENT);  // Center align

        // Condition label
        conditionLabel = new JLabel("Moderate Rain", SwingConstants.CENTER);
        conditionLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        conditionLabel.setForeground(new Color(51, 102, 204));  // Dark blue font
        conditionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);  // Center align

        // Humidity label
        humidityLabel = new JLabel("Humidity: 96%", SwingConstants.CENTER);  
        humidityLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        humidityLabel.setForeground(new Color(102, 102, 102));  // Dark gray
        humidityLabel.setAlignmentX(Component.CENTER_ALIGNMENT);  // Center align

        // Wind speed label
        windSpeedLabel = new JLabel("Wind Speed: 34 km/h", SwingConstants.CENTER);  
        windSpeedLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        windSpeedLabel.setForeground(new Color(102, 102, 102));  // Dark gray
        windSpeedLabel.setAlignmentX(Component.CENTER_ALIGNMENT);  // Center align

        // Add weather details to weather panel
        weatherPanel.add(Box.createVerticalStrut(10));  // Spacer
        weatherPanel.add(dateTimeLabel);
        weatherPanel.add(Box.createVerticalStrut(10));  // Spacer
        weatherPanel.add(weatherIconLabel);
        weatherPanel.add(Box.createVerticalStrut(10));  // Spacer
        weatherPanel.add(temperatureLabel);
        weatherPanel.add(Box.createVerticalStrut(5));  // Spacer
        weatherPanel.add(conditionLabel);
        weatherPanel.add(humidityLabel);  // Add humidity
        weatherPanel.add(windSpeedLabel);  // Add wind speed

        // Add weather panel below search panel (centered)
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        add(weatherPanel, gbc);

        // Make the UI visible
        setVisible(true);
    }

    // Method to fetch weather data asynchronously
    private void fetchWeatherData() {
        System.out.println("Fetching weather data...");
        String city = cityInput.getText().trim();
        if (city.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a city name!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                String weatherData = getWeatherData(city);
                System.out.println("API Response: " + weatherData);  // Log the response
                SwingUtilities.invokeLater(() -> updateWeatherUI(weatherData));
            } catch (IOException | InterruptedException ex) {
                SwingUtilities.invokeLater(() -> {
                    conditionLabel.setText("Error fetching weather data.");
                });
            }
        });
    }

    // Method to fetch weather data
    private String getWeatherData(String city) throws IOException, InterruptedException {
        String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8.toString());
        String urlString = String.format("%s?key=%s&q=%s&aqi=no", WEATHER_BASE_URL, WEATHER_API_KEY, encodedCity);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(urlString)).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return response.body();
        } else {
            return "Error";
        }
    }

    // Method to update the UI based on the weather data
    private void updateWeatherUI(String weatherData) {
        if (!weatherData.equals("Error")) {
            // Parse weather data
            String tempKeyword = "\"temp_c\":";
            String humidityKeyword = "\"humidity\":";
            String conditionKeyword = "\"text\":\"";
            String windSpeedKeyword = "\"wind_kph\":";

            // Extract data
            String temperature = extractValue(weatherData, tempKeyword);
            String humidity = extractValue(weatherData, humidityKeyword);
            String condition = extractValue(weatherData, conditionKeyword);
            String windSpeed = extractValue(weatherData, windSpeedKeyword);

            // Update UI components
            temperatureLabel.setText(temperature + "°C");
            conditionLabel.setText(condition);
            humidityLabel.setText("Humidity: " + humidity + "%");
            windSpeedLabel.setText("Wind Speed: " + windSpeed + " km/h");

            // Update the weather icon based on the condition
            updateWeatherIcon(condition);
        }
    }

    // Helper method to extract values from the JSON response
    private String extractValue(String data, String keyword) {
        int startIndex = data.indexOf(keyword) + keyword.length();
        int endIndex = data.indexOf(",", startIndex);
        if (endIndex == -1) {  // In case it's the last value in the JSON and no comma follows
            endIndex = data.indexOf("}", startIndex);
        }
        return data.substring(startIndex, endIndex).replace("\"", "");
    }

    // Method to update the weather icon based on the condition
    private void updateWeatherIcon(String condition) {
        switch (condition.toLowerCase()) {
            case "clear":
                weatherIconLabel.setIcon(new ImageIcon(getClass().getResource("/com/example/images/sunny.png")));
                break;
            case "rain":
                weatherIconLabel.setIcon(new ImageIcon(getClass().getResource("/com/example/images/rainy.png")));
                break;
            case "clouds":
            case "overcast":
                weatherIconLabel.setIcon(new ImageIcon(getClass().getResource("/com/example/images/cloudy.png")));  // Placeholder for cloudy
                break;
            default:
                weatherIconLabel.setIcon(new ImageIcon(getClass().getResource("/com/example/images/default.png")));  // Default weather icon
                break;
        }
    }

    public static void main(String[] args) {
        // Run the UI in the event dispatch thread
        SwingUtilities.invokeLater(WeatherAppUI::new);
    }

    // Rounded panel class to create a rounded shape around the UI
    static class RoundedPanel extends JPanel {
        private int cornerRadius;
        private Color backgroundColor;

        public RoundedPanel(int radius, Color bgColor) {
            super();
            this.cornerRadius = radius;
            this.backgroundColor = bgColor;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(backgroundColor);
            g2d.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);
        }
    }
}
