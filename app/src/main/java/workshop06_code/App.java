package workshop06_code;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class App {
    private static final Logger logger = Logger.getLogger(App.class.getName());

    static {
        try {
            InputStream is = App.class.getClassLoader().getResourceAsStream("logging.properties");
            if (is != null) {
                LogManager.getLogManager().readConfiguration(is);
                logger.info("Logging configured successfully in App.");
            } else {
                logger.severe("Could not find logging.properties on classpath");
            }
        } catch (SecurityException | IOException e) {
            logger.log(Level.SEVERE, "Failed to configure logging: " + e.getMessage(), e);
        }
    }

    public static void main(String[] args) {
        SQLiteConnectionManager wordleDatabaseConnection = new SQLiteConnectionManager("words.db");

        wordleDatabaseConnection.createNewDatabase("words.db");
        if (!wordleDatabaseConnection.checkIfConnectionDefined()) {
            logger.severe("Database connection failed.");
            System.out.println("Sorry, the game cannot start due to a technical issue.");
            return;
        }

        if (!wordleDatabaseConnection.createWordleTables()) {
            logger.severe("Failed to set up game tables.");
            System.out.println("Sorry, the game cannot start due to a technical issue.");
            return;
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(App.class.getClassLoader().getResourceAsStream("data.txt")))) {
            String line;
            int i = 1;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.length() == 4) {
                    logger.info("Valid word loaded: " + line);
                    wordleDatabaseConnection.addValidWord(i, line);
                    i++;
                } else {
                    logger.severe("Invalid word in data.txt: '" + line + "' (must be 4 letters)");
                }
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to load words from data.txt: " + e.getMessage(), e);
            System.out.println("Sorry, the game cannot start due to a problem loading words.");
            return;
        }

        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("Enter a 4 letter word for a guess or q to quit: ");
            String guess = scanner.nextLine();

            while (!guess.equals("q")) {
                if (guess.length() != 4) {
                    logger.info("Invalid guess attempt: '" + guess + "' (not 4 letters)");
                    System.out.println("Please enter a 4-letter word.");
                } else {
                    System.out.println("You've guessed '" + guess + "'.");
                    if (wordleDatabaseConnection.isValidWord(guess)) {
                        System.out.println("Success! It is in the list.\n");
                    } else {
                        logger.info("Valid but incorrect guess: '" + guess + "'");
                        System.out.println("Sorry. This word is NOT in the list.\n");
                    }
                }
                System.out.print("Enter a 4 letter word for a guess or q to quit: ");
                guess = scanner.nextLine();
            }
        } catch (NoSuchElementException | IllegalStateException e) {
            logger.log(Level.WARNING, "Error reading user input: " + e.getMessage(), e);
            System.out.println("An error occurred while reading your input. Exiting game.");
        }
    }
}