import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import javax.swing.*;
import java.util.concurrent.CompletableFuture;

public class WeatherAndVideoSearchApp extends JFrame {

    private static final String WEATHER_API_KEY = "3d29ec11647646df94e93355240410";  // Replace with your actual weather API key
    private static final String YOUTUBE_API_KEY = "AIzaSyC7ljyli73aUmMzgoxlH86cmD-7iDyRGKA";  // Replace with your actual YouTube API key

    private static final String WEATHER_BASE_URL = "https://api.weatherapi.com/v1/current.json";
    private static final String YOUTUBE_SEARCH_URL = "https://www.googleapis.com/youtube/v3/search";

    // UI components for weather search
    private JTextField cityInput;
    private JButton fetchWeatherButton;
    private JTextArea weatherOutput;

    // UI components for video search
    private JTextField videoSearchInput;
    private JButton searchVideoButton;
    private JTextArea videoOutput;

    public WeatherAndVideoSearchApp() {
        // Set up the UI
        setTitle("Weather and Video Search Application");
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);  // Center the window

        // Set layout to GridBagLayout
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Weather search section
        cityInput = new JTextField(20);
        fetchWeatherButton = new JButton("Get Weather");
        weatherOutput = new JTextArea(5, 40);
        weatherOutput.setLineWrap(true);
        weatherOutput.setWrapStyleWord(true);
        weatherOutput.setEditable(false);
        JScrollPane weatherScrollPane = new JScrollPane(weatherOutput);

        // Video search section
        videoSearchInput = new JTextField(20);
        searchVideoButton = new JButton("Search Video");
        videoOutput = new JTextArea(10, 40);  // Increased height for better display of multiple video results
        videoOutput.setLineWrap(true);
        videoOutput.setWrapStyleWord(true);
        videoOutput.setEditable(false);
        JScrollPane videoScrollPane = new JScrollPane(videoOutput);

        // Add weather input and button to panel
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(new JLabel("Enter city/country for weather:"), gbc);

        gbc.gridx = 1;
        add(cityInput, gbc);

        gbc.gridx = 2;
        add(fetchWeatherButton, gbc);

        // Add weather output to panel
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 3;
        add(weatherScrollPane, gbc);

        // Add video input and button to panel
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        add(new JLabel("Search for a video:"), gbc);

        gbc.gridx = 1;
        add(videoSearchInput, gbc);

        gbc.gridx = 2;
        add(searchVideoButton, gbc);

        // Add video output to panel
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 3;
        add(videoScrollPane, gbc);

        // Add action listeners for buttons
        fetchWeatherButton.addActionListener(e -> fetchWeather());
        searchVideoButton.addActionListener(e -> searchYouTubeVideo());

        // Make the UI visible
        setVisible(true);
    }

    // Method to fetch weather data asynchronously
    private void fetchWeather() {
        String city = cityInput.getText().trim();
        weatherOutput.setText("");  // Clear output area

        if (city.isEmpty()) {
            weatherOutput.setText("Please enter a city or country name.");
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                String weatherData = getWeatherData(city);
                SwingUtilities.invokeLater(() -> weatherOutput.setText(weatherData));
            } catch (IOException | InterruptedException ex) {
                SwingUtilities.invokeLater(() -> weatherOutput.setText("Error fetching weather data: " + ex.getMessage()));
            }
        });
    }

    // Method to search for YouTube videos asynchronously
    private void searchYouTubeVideo() {
        String query = videoSearchInput.getText().trim();
        videoOutput.setText("");  // Clear output area

        if (query.isEmpty()) {
            videoOutput.setText("Please enter a search term.");
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                String videoData = getYouTubeVideos(query);
                SwingUtilities.invokeLater(() -> videoOutput.setText(videoData));
            } catch (IOException | InterruptedException ex) {
                SwingUtilities.invokeLater(() -> videoOutput.setText("Error fetching video data: " + ex.getMessage()));
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
            String responseBody = response.body();
            
            // Extract temperature, humidity, and condition
            String tempKeyword = "\"temp_c\":";
            String humidityKeyword = "\"humidity\":";
            String conditionKeyword = "\"text\":\"";

            int tempIndex = responseBody.indexOf(tempKeyword);
            int humidityIndex = responseBody.indexOf(humidityKeyword);
            int conditionIndex = responseBody.indexOf(conditionKeyword);

            if (tempIndex != -1 && humidityIndex != -1 && conditionIndex != -1) {
                int tempStart = tempIndex + tempKeyword.length();
                int tempEnd = responseBody.indexOf(",", tempStart);
                String temperature = responseBody.substring(tempStart, tempEnd);

                int humidityStart = humidityIndex + humidityKeyword.length();
                int humidityEnd = responseBody.indexOf(",", humidityStart);
                String humidity = responseBody.substring(humidityStart, humidityEnd);

                int conditionStart = conditionIndex + conditionKeyword.length();
                int conditionEnd = responseBody.indexOf("\"", conditionStart);
                String condition = responseBody.substring(conditionStart, conditionEnd);

                return String.format("Weather data for %s:\nTemperature (C): %s\nHumidity: %s%%\nCondition: %s", city, temperature, humidity, condition);
            } else {
                return "Could not find complete weather information.";
            }
        } else {
            return "Error: Unable to fetch weather data. HTTP Status Code: " + response.statusCode();
        }
    }

    // Method to fetch YouTube video search results with multiple videos and details
    private String getYouTubeVideos(String query) throws IOException, InterruptedException {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
        String urlString = String.format("%s?part=snippet&q=%s&type=video&key=%s&maxResults=5", 
                                         YOUTUBE_SEARCH_URL, encodedQuery, YOUTUBE_API_KEY);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(urlString)).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            String responseBody = response.body();
            StringBuilder videoResults = new StringBuilder();
            int videoCount = 1;

            // Loop through and extract multiple video results (title, description, videoId)
            String videoKeyword = "\"videoId\":\"";
            String titleKeyword = "\"title\":\"";
            String descriptionKeyword = "\"description\":\"";

            int index = 0;
            while ((index = responseBody.indexOf(videoKeyword, index)) != -1 && videoCount <= 5) {
                // Get Video ID
                int startIndex = index + videoKeyword.length();
                int endIndex = responseBody.indexOf("\"", startIndex);
                String videoId = responseBody.substring(startIndex, endIndex);

                // Get Title
                int titleIndex = responseBody.indexOf(titleKeyword, endIndex);
                int titleStart = titleIndex + titleKeyword.length();
                int titleEnd = responseBody.indexOf("\"", titleStart);
                String title = responseBody.substring(titleStart, titleEnd);

                // Get Description
                int descriptionIndex = responseBody.indexOf(descriptionKeyword, titleEnd);
                int descriptionStart = descriptionIndex + descriptionKeyword.length();
                int descriptionEnd = responseBody.indexOf("\"", descriptionStart);
                String description = responseBody.substring(descriptionStart, descriptionEnd);

                // Format result for each video
                videoResults.append("Video ").append(videoCount).append(":\n");
                videoResults.append("Title: ").append(title).append("\n");
                videoResults.append("Description: ").append(description).append("\n");
                videoResults.append("URL: https://www.youtube.com/watch?v=").append(videoId).append("\n\n");

                videoCount++;
                index = endIndex;
            }

            return videoResults.toString();
        } else {
            return "Error: Unable to fetch video data. HTTP Status Code: " + response.statusCode();
        }
    }

    public static void main(String[] args) {
        // Run the UI in the event dispatch thread
        SwingUtilities.invokeLater(WeatherAndVideoSearchApp::new);
    }
}
