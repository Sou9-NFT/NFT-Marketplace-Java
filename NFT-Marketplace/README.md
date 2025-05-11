# NFT Marketplace Java

A modern NFT Marketplace desktop application built with JavaFX, supporting NFT creation, trading, raffles, and social features. This project demonstrates a full-featured NFT platform with a beautiful UI, chatbot assistant, and integration with external APIs.

## Features
- **NFT Creation & Management**: Create, view, and manage digital artworks as NFTs.
- **Raffle System**: Create and join raffles to win NFTs. Automated winner selection and participant management.
- **Trade Offers**: Propose and manage NFT trades between users.
- **User Dashboard**: Track your NFTs, raffles, trades, and statistics.
- **Chatbot Assistant**: Gemini-powered chatbot to guide users and answer questions about the platform.
- **Social Sharing**: Share raffle wins directly to X (Twitter).
- **PDF Reports**: Generate PDF reports for raffles and trades.
- **Modern UI**: Built with JavaFX and custom CSS for a sleek, responsive experience.

## Requirements
- **Java 17** (required)
- **Maven** (for dependency management)
- **MySQL** (for database)

## Setup & Installation
1. **Clone the repository**
   ```
   git clone https://github.com/yourusername/NFT-Marketplace-Java.git
   ```
2. **Configure the database**
   - Create a MySQL database and run `NFT-Marketplace/database_init.sql` to set up tables.
   - Update `src/main/resources/config.properties` with your API keys and Twilio credentials.
3. **Build the project**
   ```
   cd NFT-Marketplace
   mvn clean install
   ```
4. **Run the application**
   ```
   mvn javafx:run
   ```

## Usage
- **Login/Register**: Start the app and create a user account or log in.
- **NFTs**: Go to the Artworks section to create or view NFTs.
- **Raffles**: Join or create raffles from the Raffles page. Winners are selected automatically.
- **Trades**: Propose trades with other users for NFT exchanges.
- **Chatbot**: Use the built-in chatbot for help and platform guidance.
- **Share Wins**: After winning a raffle, use the 'Share on X' button to post your victory.

## Project Structure
- `src/main/java/org/esprit/` - Java source code (controllers, models, services, utils)
- `src/main/resources/` - FXML layouts, CSS styles, config files, assets
- `database_init.sql` - SQL script to initialize the database
- `pom.xml` - Maven dependencies and build configuration

## Main Technologies
- Java 17, JavaFX, Maven
- MySQL, iText PDF, Twilio, Twitter4J, Gemini API

## License
This project is for educational purposes.
