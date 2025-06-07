module pt.goncalo3.batalhanaval {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires java.net.http;    // Add for HttpClient
    requires java.prefs;       // Add for Preferences
    requires org.json;         // Add for JSON handling
    requires Java.WebSocket;   // Add for WebSocket client
    requires com.fasterxml.jackson.databind;  // Add for Jackson JSON processing

    opens pt.goncalo3.batalhanaval to javafx.fxml;
    exports pt.goncalo3.batalhanaval;
}

