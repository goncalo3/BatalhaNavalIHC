package pt.goncalo3.batalhanaval;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

/**
 * Controller for the login/registration page of the Battleship game
 */
public class LoginController {

    @FXML private Label titleLabel;
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private VBox emailContainer;
    @FXML private PasswordField passwordField;
    @FXML private Label errorMessageLabel;
    @FXML private Button submitButton;
    @FXML private Label switchModeLabel;
    @FXML private Hyperlink switchModeLink;

    private boolean isRegistrationMode = false;
    private final User user = User.getInstance();

    /**
     * Initialize the controller.
     * Called automatically after FXML elements are injected.
     */
    @FXML
    public void initialize() {
        updateUIForMode();
    }

    /**
     * Handle the submit button click (login or register).
     */
    @FXML
    public void onSubmitButtonClick(ActionEvent event) {
        hideErrorMessage();

        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showErrorMessage("Please fill in all required fields");
            return;
        }

        try {
            boolean success;

            if (isRegistrationMode) {
                String email = emailField.getText().trim();
                if (email.isEmpty()) {
                    showErrorMessage("Please enter your email address");
                    return;
                }

                success = user.register(username, email, password);
                // Registration returns a token/user data so no explicit login needed afterward
            } else {
                success = user.login(username, password);
            }

            if (success) {
                System.out.println("Authentication successful, navigating to home page...");
                navigateToHomePage();
            } else {
                String errorMessage = user.getLastErrorMessage();
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = isRegistrationMode
                            ? "Registration failed. Please try again."
                            : "Login failed. Check your credentials.";
                }
                showErrorMessage(errorMessage);
            }
        } catch (IOException | InterruptedException e) {
            String errorMessage = user.getLastErrorMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "Connection error: " + e.getMessage();
            }
            showErrorMessage(errorMessage);
            e.printStackTrace();
        }
    }

    /**
     * Switch between login and registration modes.
     */
    @FXML
    public void onSwitchModeClick(ActionEvent event) {
        isRegistrationMode = !isRegistrationMode;
        updateUIForMode();
    }

    /**
     * Navigate back to the home page (triggered by the "Back to Home" hyperlink).
     */
    @FXML
    public void onBackToHomeClick(ActionEvent event) throws IOException {
        // 1) Locate home-view.fxml via its absolute classpath location
        URL fxmlUrl = getClass().getResource("/pt/goncalo3/batalhanaval/fxml/home-view.fxml");
        if (fxmlUrl == null) {
            throw new RuntimeException(
                    "Cannot find home-view.fxml at '/pt/goncalo3/batalhanaval/fxml/home-view.fxml'"
            );
        }

        // 2) Load the Home scene
        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        Parent homeRoot = loader.load();

        // 3) Get the HomeController and refresh its UI (so buttons/username update correctly)
        HomeController homeController = loader.getController();
        homeController.refreshUI();

        // 4) Replace the current Scene with the Home scene
        Stage stage = (Stage) submitButton.getScene().getWindow();
        Scene scene = new Scene(homeRoot, 600, 800);

        // 5) Attach your stylesheet via absolute path
        URL cssUrl = getClass().getResource("/pt/goncalo3/batalhanaval/styles.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

        stage.setTitle("Battleship");
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Allow external code (e.g. HomeController) to set registration mode.
     * @param registrationMode true to switch into registration mode
     */
    public void setRegistrationMode(boolean registrationMode) {
        this.isRegistrationMode = registrationMode;
        updateUIForMode();
    }

    /**
     * Update all UI text and visibility based on the current mode (login vs. register).
     */
    private void updateUIForMode() {
        if (isRegistrationMode) {
            titleLabel.setText("Welcome to Battleship 👋");
            submitButton.setText("REGISTER");
            switchModeLabel.setText("Already have an account?");
            switchModeLink.setText("Login here");
            emailContainer.setVisible(true);
            emailContainer.setManaged(true);
        } else {
            titleLabel.setText("Welcome back! 👋");
            submitButton.setText("Login");
            switchModeLabel.setText("Don't have an account?");
            switchModeLink.setText("Sign up");
            emailContainer.setVisible(false);
            emailContainer.setManaged(false);
        }
    }

    /**
     * After a successful login or registration, navigate to the home page.
     */
    private void navigateToHomePage() throws IOException {
        // Exactly the same logic as onBackToHomeClick:
        URL fxmlUrl = getClass().getResource("/pt/goncalo3/batalhanaval/fxml/home-view.fxml");
        if (fxmlUrl == null) {
            throw new RuntimeException(
                    "Cannot find home-view.fxml at '/pt/goncalo3/batalhanaval/fxml/home-view.fxml'"
            );
        }
        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        Parent root = loader.load();

        HomeController homeController = loader.getController();
        homeController.refreshUI();

        Stage stage = (Stage) submitButton.getScene().getWindow();
        Scene scene = new Scene(root, 600, 800);

        URL cssUrl = getClass().getResource("/pt/goncalo3/batalhanaval/styles.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

        stage.setTitle("Battleship");
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Display an error message beneath the fields.
     */
    private void showErrorMessage(String message) {
        errorMessageLabel.setText(message);
        errorMessageLabel.setVisible(true);
    }

    /**
     * Hide any previously shown error message.
     */
    private void hideErrorMessage() {
        errorMessageLabel.setVisible(false);
    }
}
