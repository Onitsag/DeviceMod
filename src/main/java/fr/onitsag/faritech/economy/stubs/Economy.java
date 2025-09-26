package fr.onitsag.faritech.economy.stubs;

/**
 * Stub pour Vault Economy en mode développement
 * Remplacé par la vraie interface Vault sur Magma
 */
public interface Economy {
    double getBalance(String player);
    EconomyResponse withdrawPlayer(String player, double amount);
    EconomyResponse depositPlayer(String player, double amount);
    boolean has(String player, double amount);
    String currencyNameSingular();
    boolean hasAccount(String player);
}
