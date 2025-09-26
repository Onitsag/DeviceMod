package fr.onitsag.faritech.economy;

import fr.onitsag.faritech.FariTechMod;
import fr.onitsag.faritech.programs.business.service.BusinessRepository;
import fr.onitsag.faritech.programs.business.model.Company;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.util.Optional;

/**
 * Gestionnaire d'économie pour FariTech
 * Intègre Vault pour l'argent des joueurs, garde l'argent des entreprises en interne
 */
public class EconomyManager {
    
    private static VaultIntegration vaultIntegration;
    private static boolean vaultAvailable = false;
    
    /**
     * Initialise l'économie et détecte Vault
     */
    public static void init() {
        System.out.println("[EconomyManager] Initialisation côté: " + (FMLCommonHandler.instance().getSide().isClient() ? "CLIENT" : "SERVEUR"));

        // Seulement côté serveur
        if (FMLCommonHandler.instance().getSide().isClient()) {
            System.out.println("[EconomyManager] Côté client - pas d'initialisation Vault");
            return;
        }

        System.out.println("[EconomyManager] Recherche de Vault via Bukkit ServicesManager...");
        vaultIntegration = new VaultIntegration();
        try {
            vaultAvailable = vaultIntegration.setupEconomy();
        } catch (Throwable t) {
            vaultAvailable = false;
            System.out.println("[EconomyManager] Erreur setupEconomy: " + t.getMessage());
        }

        if (vaultAvailable) {
            if (vaultIntegration.isUsingRealVault()) {
                FariTechMod.logger.info("Vault détecté - Intégration économie activée");
                System.out.println("[EconomyManager] ✓ Vault réel configuré");
            } else {
                FariTechMod.logger.info("Mode économie de développement (stubs)");
                System.out.println("[EconomyManager] Mode dev/stub actif (Vault non disponible)");
            }
        } else {
            FariTechMod.logger.warn("Aucun provider économique détecté (Vault) - désactivé");
            System.out.println("[EconomyManager] ✗ Aucun provider économique détecté");
        }
    }
    
    /**
     * Essaie de réinitialiser Vault si pas encore disponible (appel tardif)
     */
    private static long lastLateInitAttemptMs = 0L;
    public static void tryLateInit() {
        if (vaultAvailable) return;
        if (FMLCommonHandler.instance().getSide().isClient()) return;
        long now = System.currentTimeMillis();
        // anti-spam: pas plus d'une tentative toutes les 5 secondes
        if (now - lastLateInitAttemptMs < 5000) return;
        lastLateInitAttemptMs = now;
        System.out.println("[EconomyManager] Tentative d'initialisation tardive de Vault...");
        init();
    }
    
    /**
     * Récupère le solde d'un joueur
     */
    public static double getPlayerBalance(String playerUuid) {
        System.out.println("[EconomyManager] getPlayerBalance appelé pour UUID: " + playerUuid);
        System.out.println("[EconomyManager] vaultAvailable: " + vaultAvailable);
        
        // Tentative d'initialisation tardive si Vault pas encore disponible
        if (!vaultAvailable) {
            tryLateInit();
        }
        
        if (vaultAvailable) {
            EntityPlayer player = getPlayerByUuid(playerUuid);
            if (player != null) {
                System.out.println("[EconomyManager] Joueur trouvé: " + player.getName());
                return vaultIntegration.getBalance(player.getName());
            } else {
                System.err.println("[EconomyManager] Joueur non trouvé pour UUID: " + playerUuid);
            }
        }
        
        // Mode développement : solde simulé
        System.out.println("[EconomyManager] Mode développement - retour 10000.0");
        return 10000.0;
    }

    /**
     * Récupère le solde d'un joueur par nom (plus compatible avec Vault)
     */
    public static double getPlayerBalanceByName(String playerName) {
        if (!vaultAvailable) tryLateInit();
        if (vaultAvailable && playerName != null && !playerName.isEmpty()) {
            return vaultIntegration.getBalance(playerName);
        }
        return 10000.0;
    }
    
    /**
     * Retire de l'argent du compte d'un joueur
     */
    public static boolean withdrawFromPlayer(String playerUuid, double amount) {
        if (amount <= 0) return false;
        
        if (vaultAvailable) {
            EntityPlayer player = getPlayerByUuid(playerUuid);
            if (player != null) {
                return vaultIntegration.withdraw(player.getName(), amount);
            }
            return false;
        }
        
        // Mode développement : toujours possible
        return true;
    }

    /**
     * Retire de l'argent d'un joueur par nom
     */
    public static boolean withdrawFromPlayerByName(String playerName, double amount) {
        if (amount <= 0) return false;
        if (!vaultAvailable) tryLateInit();
        if (!vaultAvailable) return true; // mode dev
        return playerName != null && !playerName.isEmpty() && vaultIntegration.withdraw(playerName, amount);
    }
    
    /**
     * Ajoute de l'argent au compte d'un joueur
     */
    public static boolean depositToPlayer(String playerUuid, double amount) {
        if (amount <= 0) return false;
        
        if (vaultAvailable) {
            EntityPlayer player = getPlayerByUuid(playerUuid);
            if (player != null) {
                return vaultIntegration.deposit(player.getName(), amount);
            }
            return false;
        }
        
        // Mode développement : toujours possible
        return true;
    }

    /**
     * Ajoute de l'argent à un joueur par nom
     */
    public static boolean depositToPlayerByName(String playerName, double amount) {
        if (amount <= 0) return false;
        if (!vaultAvailable) tryLateInit();
        if (!vaultAvailable) return true; // mode dev
        return playerName != null && !playerName.isEmpty() && vaultIntegration.deposit(playerName, amount);
    }
    
    /**
     * Transfère de l'argent du joueur vers l'entreprise
     */
    public static boolean transferToCompany(String playerUuid, String companyId, double amount) {
        if (amount <= 0) return false;
        
        // Vérifier que l'entreprise existe
        Optional<Company> companyOpt = BusinessRepository.get().getCompany(companyId);
        if (!companyOpt.isPresent()) return false;
        
        // Retirer de l'argent du joueur
        if (withdrawFromPlayer(playerUuid, amount)) {
            // Ajouter à l'entreprise
            Company company = companyOpt.get();
            company.setBalance(company.getBalance() + amount);
            
            // Enregistrer la transaction dans l'historique de l'entreprise
            company.getTransactions().add(new fr.onitsag.faritech.programs.business.model.BizTransaction(
                java.util.UUID.randomUUID().toString(),
                System.currentTimeMillis(),
                "PERSONAL_" + playerUuid, // Compte personnel comme source
                companyId,
                amount,
                fr.onitsag.faritech.programs.business.model.BizTransaction.Type.TRANSFER_IN,
                "Dépôt de " + getPlayerName(playerUuid)
            ));
            
            // Les changements sont automatiquement persistés côté client
            
            FariTechMod.logger.info("Transfert de " + amount + "€ du joueur " + playerUuid + " vers l'entreprise " + companyId);
            return true;
        }
        
        return false;
    }
    
    /**
     * Transfère de l'argent de l'entreprise vers le joueur
     */
    public static boolean withdrawFromCompany(String companyId, String playerUuid, double amount) {
        if (amount <= 0) return false;
        
        // Vérifier que l'entreprise existe et a les fonds
        Optional<Company> companyOpt = BusinessRepository.get().getCompany(companyId);
        if (!companyOpt.isPresent()) return false;
        
        Company company = companyOpt.get();
        if (company.getBalance() < amount) return false;
        
        // Retirer de l'entreprise
        company.setBalance(company.getBalance() - amount);
        
        // Ajouter au joueur
        if (depositToPlayer(playerUuid, amount)) {
            // Enregistrer la transaction dans l'historique de l'entreprise
            company.getTransactions().add(new fr.onitsag.faritech.programs.business.model.BizTransaction(
                java.util.UUID.randomUUID().toString(),
                System.currentTimeMillis(),
                companyId,
                "PERSONAL_" + playerUuid, // Compte personnel comme destination
                amount,
                fr.onitsag.faritech.programs.business.model.BizTransaction.Type.TRANSFER_OUT,
                "Retrait de " + getPlayerName(playerUuid)
            ));
            
            // Les changements sont automatiquement persistés côté client
            
            FariTechMod.logger.info("Retrait de " + amount + "€ de l'entreprise " + companyId + " vers le joueur " + playerUuid);
            return true;
        } else {
            // Restaurer le solde de l'entreprise en cas d'échec
            company.setBalance(company.getBalance() + amount);
            return false;
        }
    }
    
    /**
     * Récupère un joueur par son UUID
     */
    private static EntityPlayer getPlayerByUuid(String playerUuid) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server != null) {
            return server.getPlayerList().getPlayerByUUID(java.util.UUID.fromString(playerUuid));
        }
        return null;
    }
    
    /**
     * Récupère le nom d'un joueur par son UUID
     */
    private static String getPlayerName(String playerUuid) {
        EntityPlayer player = getPlayerByUuid(playerUuid);
        return player != null ? player.getName() : "Joueur inconnu";
    }
    
    /**
     * Vérifie si Vault est disponible
     */
    public static boolean isVaultAvailable() {
        return vaultAvailable;
    }
    
    /**
     * Formate un montant en devise
     */
    public static String formatMoney(double amount) {
        return String.format("%.2f€", amount);
    }
}
