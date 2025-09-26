package fr.onitsag.faritech.economy;

/**
 * Façade simple et réutilisable pour les opérations Vault
 * Fournit des helpers par UUID et par nom de joueur.
 */
public final class VaultEconomy {

    private VaultEconomy() {}

    // --- Lecture solde ---
    public static double getBalanceByUuid(String playerUuid) {
        return EconomyManager.getPlayerBalance(playerUuid);
    }

    public static double getBalanceByName(String playerName) {
        return EconomyManager.getPlayerBalanceByName(playerName);
    }

    // --- Retrait ---
    public static boolean takeByUuid(String playerUuid, double amount) {
        return EconomyManager.withdrawFromPlayer(playerUuid, amount);
    }

    public static boolean takeByName(String playerName, double amount) {
        return EconomyManager.withdrawFromPlayerByName(playerName, amount);
    }

    // --- Dépôt ---
    public static boolean giveByUuid(String playerUuid, double amount) {
        return EconomyManager.depositToPlayer(playerUuid, amount);
    }

    public static boolean giveByName(String playerName, double amount) {
        return EconomyManager.depositToPlayerByName(playerName, amount);
    }

    // --- Vérifications ---
    public static boolean isAvailable() {
        return EconomyManager.isVaultAvailable();
    }

    public static String format(double amount) {
        return EconomyManager.formatMoney(amount);
    }
}


