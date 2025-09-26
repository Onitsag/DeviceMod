package fr.onitsag.faritech.economy;

import fr.onitsag.faritech.economy.stubs.Economy;
import fr.onitsag.faritech.economy.stubs.EconomyResponse;
import java.lang.reflect.Method;

/**
 * Intégration avec Vault pour l'économie des joueurs
 * Utilise la réflexion pour s'adapter automatiquement selon l'environnement:
 * - Mode dev: utilise les stubs et soldes simulés
 * - Production avec Magma: utilise Vault réel
 */
public class VaultIntegration {
    
    private Object economy = null;
    private boolean isRealVault = false;
    private Class<?> economyClass = null;
    private Class<?> economyResponseClass = null;
    
    /**
     * Configure l'intégration avec Vault
     * @return true si Vault et un plugin économique sont disponibles
     */
    public boolean setupEconomy() {
        try {
            System.out.println("[VaultIntegration] Tentative d'initialisation avec Vault réel...");
            
            // Tentative d'utilisation de Vault réel (Magma)
            Class<?> bukkitClass = Class.forName("org.bukkit.Bukkit");
            Method getServerMethod = bukkitClass.getDeclaredMethod("getServer");
            Object server = getServerMethod.invoke(null);
            
            if (server != null) {
                System.out.println("[VaultIntegration] Serveur Bukkit détecté");
                Method getServicesManagerMethod = server.getClass().getDeclaredMethod("getServicesManager");
                Object servicesManager = getServicesManagerMethod.invoke(server);

                // Charger les classes Vault
                try {
                    economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
                    economyResponseClass = Class.forName("net.milkbowl.vault.economy.EconomyResponse");
                } catch (ClassNotFoundException cnf) {
                    // Fallback: essayer via le classloader du plugin Vault
                    try {
                        Method getPluginManagerMethod = server.getClass().getDeclaredMethod("getPluginManager");
                        Object pluginManager = getPluginManagerMethod.invoke(server);
                        Method getPluginMethod = pluginManager.getClass().getDeclaredMethod("getPlugin", String.class);
                        Object vaultPlugin = getPluginMethod.invoke(pluginManager, "Vault");
                        if (vaultPlugin != null) {
                            ClassLoader cl = vaultPlugin.getClass().getClassLoader();
                            economyClass = cl.loadClass("net.milkbowl.vault.economy.Economy");
                            economyResponseClass = cl.loadClass("net.milkbowl.vault.economy.EconomyResponse");
                            System.out.println("[VaultIntegration] Classes Vault chargées via le plugin Vault");
                        } else {
                            System.err.println("[VaultIntegration] Plugin Vault introuvable");
                            return false;
                        }
                    } catch (Exception inner) {
                        System.err.println("[VaultIntegration] Impossible de charger les classes Vault: " + inner.getMessage());
                        return false;
                    }
                }

                Method getRegistrationMethod = servicesManager.getClass().getDeclaredMethod("getRegistration", Class.class);
                Object rsp = getRegistrationMethod.invoke(servicesManager, economyClass);
                
                if (rsp != null) {
                    Method getProviderMethod = rsp.getClass().getDeclaredMethod("getProvider");
                    economy = getProviderMethod.invoke(rsp);
                    isRealVault = true;
                    System.out.println("[VaultIntegration] Vault réel configuré avec succès!");
                    return economy != null;
                } else {
                    System.err.println("[VaultIntegration] Aucun provider d'économie trouvé via Vault");
                }
            } else {
                System.err.println("[VaultIntegration] Serveur Bukkit non disponible");
            }
        } catch (Exception e) {
            System.out.println("[VaultIntegration] Échec de Vault réel: " + e.getMessage());
            // Fallback en mode développement avec stubs
            isRealVault = false;
            economy = new DevEconomyStub();
            System.out.println("[VaultIntegration] Fallback vers mode développement");
            return true;
        }
        return false;
    }

    public boolean isUsingRealVault() {
        return isRealVault;
    }
    
    /**
     * Récupère le solde d'un joueur
     */
    public double getBalance(String playerName) {
        if (economy == null) {
            System.err.println("[VaultIntegration] Economy is null pour getBalance(" + playerName + ")");
            return 0.0;
        }
        
        try {
            if (isRealVault) {
                // Essayer Economy#getBalance(String)
                try {
                    Method getBalanceMethod = economyClass.getDeclaredMethod("getBalance", String.class);
                    double balance = (Double) getBalanceMethod.invoke(economy, playerName);
                    System.out.println("[VaultIntegration] Vault réel - Solde de " + playerName + ": " + balance);
                    return balance;
                } catch (NoSuchMethodException nsme) {
                    // Fallback: Economy#getBalance(OfflinePlayer)
                    Class<?> offlinePlayerClass = Class.forName("org.bukkit.OfflinePlayer");
                    Method getOfflinePlayer = Class.forName("org.bukkit.Bukkit").getDeclaredMethod("getOfflinePlayer", String.class);
                    Object offline = getOfflinePlayer.invoke(null, playerName);
                    Method getBalanceMethod2 = economyClass.getDeclaredMethod("getBalance", offlinePlayerClass);
                    double balance = (Double) getBalanceMethod2.invoke(economy, offline);
                    System.out.println("[VaultIntegration] Vault réel(OP) - Solde de " + playerName + ": " + balance);
                    return balance;
                }
            } else {
                double balance = ((DevEconomyStub) economy).getBalance(playerName);
                System.out.println("[VaultIntegration] Mode dev - Solde de " + playerName + ": " + balance);
                return balance;
            }
        } catch (Exception e) {
            System.err.println("[VaultIntegration] Erreur lors de la récupération du solde pour " + playerName + ": " + e.getMessage());
            e.printStackTrace();
            return 0.0;
        }
    }
    
    /**
     * Retire de l'argent du compte d'un joueur
     */
    public boolean withdraw(String playerName, double amount) {
        if (economy == null) return false;
        
        try {
            if (isRealVault) {
                Object response;
                try {
                    Method withdrawMethod = economyClass.getDeclaredMethod("withdrawPlayer", String.class, double.class);
                    response = withdrawMethod.invoke(economy, playerName, amount);
                } catch (NoSuchMethodException nsme) {
                    Class<?> offlinePlayerClass = Class.forName("org.bukkit.OfflinePlayer");
                    Method getOfflinePlayer = Class.forName("org.bukkit.Bukkit").getDeclaredMethod("getOfflinePlayer", String.class);
                    Object offline = getOfflinePlayer.invoke(null, playerName);
                    Method withdrawMethod2 = economyClass.getDeclaredMethod("withdrawPlayer", offlinePlayerClass, double.class);
                    response = withdrawMethod2.invoke(economy, offline, amount);
                }
                
                Method transactionSuccessMethod = economyResponseClass.getDeclaredMethod("transactionSuccess");
                return (Boolean) transactionSuccessMethod.invoke(response);
            } else {
                EconomyResponse response = ((DevEconomyStub) economy).withdrawPlayer(playerName, amount);
                return response.transactionSuccess();
            }
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Ajoute de l'argent au compte d'un joueur
     */
    public boolean deposit(String playerName, double amount) {
        if (economy == null) return false;
        
        try {
            if (isRealVault) {
                Object response;
                try {
                    Method depositMethod = economyClass.getDeclaredMethod("depositPlayer", String.class, double.class);
                    response = depositMethod.invoke(economy, playerName, amount);
                } catch (NoSuchMethodException nsme) {
                    Class<?> offlinePlayerClass = Class.forName("org.bukkit.OfflinePlayer");
                    Method getOfflinePlayer = Class.forName("org.bukkit.Bukkit").getDeclaredMethod("getOfflinePlayer", String.class);
                    Object offline = getOfflinePlayer.invoke(null, playerName);
                    Method depositMethod2 = economyClass.getDeclaredMethod("depositPlayer", offlinePlayerClass, double.class);
                    response = depositMethod2.invoke(economy, offline, amount);
                }
                
                Method transactionSuccessMethod = economyResponseClass.getDeclaredMethod("transactionSuccess");
                return (Boolean) transactionSuccessMethod.invoke(response);
            } else {
                EconomyResponse response = ((DevEconomyStub) economy).depositPlayer(playerName, amount);
                return response.transactionSuccess();
            }
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Vérifie si un joueur a suffisamment d'argent
     */
    public boolean has(String playerName, double amount) {
        if (economy == null) return false;
        
        try {
            if (isRealVault) {
                Method hasMethod = economyClass.getDeclaredMethod("has", String.class, double.class);
                return (Boolean) hasMethod.invoke(economy, playerName, amount);
            } else {
                return ((DevEconomyStub) economy).has(playerName, amount);
            }
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Récupère le nom de la devise
     */
    public String getCurrencyName() {
        if (economy == null) return "€";
        
        try {
            if (isRealVault) {
                Method currencyMethod = economyClass.getDeclaredMethod("currencyNameSingular");
                return (String) currencyMethod.invoke(economy);
            } else {
                return ((DevEconomyStub) economy).currencyNameSingular();
            }
        } catch (Exception e) {
            return "€";
        }
    }
    
    /**
     * Implémentation stub pour le mode développement
     */
    private static class DevEconomyStub implements Economy {
        private static final java.util.Map<String, Double> balances = new java.util.HashMap<>();
        
        @Override
        public double getBalance(String player) {
            return balances.getOrDefault(player, 10000.0); // 10k par défaut en dev
        }
        
        @Override
        public EconomyResponse withdrawPlayer(String player, double amount) {
            double balance = getBalance(player);
            if (balance >= amount) {
                balances.put(player, balance - amount);
                return new EconomyResponse(amount, balance - amount, EconomyResponse.ResponseType.SUCCESS, null);
            }
            return new EconomyResponse(amount, balance, EconomyResponse.ResponseType.FAILURE, "Solde insuffisant");
        }
        
        @Override
        public EconomyResponse depositPlayer(String player, double amount) {
            double balance = getBalance(player);
            balances.put(player, balance + amount);
            return new EconomyResponse(amount, balance + amount, EconomyResponse.ResponseType.SUCCESS, null);
        }
        
        @Override
        public boolean has(String player, double amount) {
            return getBalance(player) >= amount;
        }
        
        @Override
        public String currencyNameSingular() {
            return "€";
        }
        
        @Override
        public boolean hasAccount(String player) {
            return true; // Tous les joueurs ont un compte en dev
        }
    }
}
