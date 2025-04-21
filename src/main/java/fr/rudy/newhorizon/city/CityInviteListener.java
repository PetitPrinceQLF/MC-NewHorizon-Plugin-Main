package fr.rudy.newhorizon.city;

import fr.rudy.newhorizon.Main;
import fr.rudy.newhorizon.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.*;

public class CityInviteListener implements Listener {

    public static final Set<UUID> awaitingInviteInput = new HashSet<>();
    public static final Set<UUID> awaitingPromoteInput = new HashSet<>();
    public static final Set<UUID> awaitingDemoteInput = new HashSet<>();
    public static final Map<UUID, UUID> awaitingConfirmPromote = new HashMap<>();

    private final Main plugin = Main.get();

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player sender = event.getPlayer();
        UUID senderUUID = sender.getUniqueId();
        String input = ChatColor.stripColor(event.getMessage().trim());

        // 🔸 Invite
        if (awaitingInviteInput.remove(senderUUID)) {
            event.setCancelled(true);

            if (input.equalsIgnoreCase("quitter")) {
                MessageUtil.sendMessage(sender, plugin.getPrefixError(), "&cInvitation annulée.");
                return;
            }

            Player target = Bukkit.getPlayerExact(input);
            if (target == null || !target.isOnline()) {
                MessageUtil.sendMessage(sender, plugin.getPrefixError(), "&cJoueur introuvable ou hors ligne.");
                return;
            }

            UUID targetUUID = target.getUniqueId();
            String cityName = plugin.getCityManager().getCityName(senderUUID);
            CityRank senderRank = plugin.getCityManager().getCityRank(senderUUID);

            if (cityName == null || senderRank == null || !(senderRank == CityRank.LEADER || senderRank == CityRank.COLEADER)) {
                MessageUtil.sendMessage(sender, plugin.getPrefixError(), "&cVous n'avez pas la permission d'inviter.");
                return;
            }

            if (plugin.getCityManager().getCityName(targetUUID) != null) {
                MessageUtil.sendMessage(sender, plugin.getPrefixError(), "&cCe joueur est déjà dans une ville.");
                return;
            }

            plugin.getPendingInvites().put(targetUUID, cityName);
            MessageUtil.sendMessage(target, plugin.getPrefixInfo(), "&bVous avez été invité à rejoindre la ville &d" + cityName + "&b !");
            MessageUtil.sendMessage(target, plugin.getPrefixInfo(), "&7Faites &e/city accept &7pour accepter ou &c/city deny &7pour refuser.");
            MessageUtil.sendMessage(sender, plugin.getPrefixInfo(), "&bInvitation envoyée à &d" + target.getName() + "&b.");
            return;
        }

        // 🔸 Promote
        if (awaitingPromoteInput.remove(senderUUID)) {
            event.setCancelled(true);

            if (input.equalsIgnoreCase("quitter")) {
                MessageUtil.sendMessage(sender, plugin.getPrefixError(), "&cPromotion annulée.");
                return;
            }

            Player target = Bukkit.getPlayerExact(input);
            if (target == null || !target.isOnline()) {
                MessageUtil.sendMessage(sender, plugin.getPrefixError(), "&cJoueur introuvable ou hors ligne.");
                return;
            }

            UUID targetUUID = target.getUniqueId();
            String city = plugin.getCityManager().getCityName(senderUUID);
            CityRank senderRank = plugin.getCityManager().getCityRank(senderUUID);
            CityRank targetRank = plugin.getCityManager().getCityRank(targetUUID);

            if (city == null || senderRank != CityRank.LEADER) {
                MessageUtil.sendMessage(sender, plugin.getPrefixError(), "&cSeul le chef peut promouvoir un joueur.");
                return;
            }

            if (!city.equals(plugin.getCityManager().getCityName(targetUUID))) {
                MessageUtil.sendMessage(sender, plugin.getPrefixError(), "&cCe joueur n’est pas dans votre ville.");
                return;
            }

            if (targetRank == CityRank.COLEADER) {
                awaitingConfirmPromote.put(senderUUID, targetUUID);
                MessageUtil.sendMessage(sender, plugin.getPrefixInfo(), "&e⚠️ &bVous êtes sur le point de transférer votre rôle de chef à &d" + target.getName() + "&b.");
                MessageUtil.sendMessage(sender, plugin.getPrefixInfo(), "&7Tapez &a/city confirm &7pour confirmer ou ignorez pour annuler.");
            } else {
                boolean success = plugin.getCityManager().setMember(city, targetUUID, CityRank.COLEADER);
                if (success) {
                    MessageUtil.sendMessage(sender, plugin.getPrefixInfo(), "&b&d" + target.getName() + " &best maintenant Sous-chef !");
                    MessageUtil.sendMessage(target, plugin.getPrefixInfo(), "&bVous avez été promu Sous-chef par &d" + sender.getName() + "&b !");
                } else {
                    MessageUtil.sendMessage(sender, plugin.getPrefixError(), "&cErreur lors de la promotion.");
                }
            }
            return;
        }

        // 🔸 Demote
        if (awaitingDemoteInput.remove(senderUUID)) {
            event.setCancelled(true);

            if (input.equalsIgnoreCase("quitter")) {
                MessageUtil.sendMessage(sender, plugin.getPrefixError(), "&cRétrogradation annulée.");
                return;
            }

            Player target = Bukkit.getPlayerExact(input);
            if (target == null || !target.isOnline()) {
                MessageUtil.sendMessage(sender, plugin.getPrefixError(), "&cJoueur introuvable ou hors ligne.");
                return;
            }

            UUID targetUUID = target.getUniqueId();
            String city = plugin.getCityManager().getCityName(senderUUID);
            CityRank senderRank = plugin.getCityManager().getCityRank(senderUUID);
            CityRank targetRank = plugin.getCityManager().getCityRank(targetUUID);

            if (city == null || senderRank != CityRank.LEADER) {
                MessageUtil.sendMessage(sender, plugin.getPrefixError(), "&cSeul le chef peut rétrograder un joueur.");
                return;
            }

            if (!city.equals(plugin.getCityManager().getCityName(targetUUID))) {
                MessageUtil.sendMessage(sender, plugin.getPrefixError(), "&cCe joueur n’est pas dans votre ville.");
                return;
            }

            if (targetUUID.equals(senderUUID)) {
                MessageUtil.sendMessage(sender, plugin.getPrefixError(), "&cVous ne pouvez pas vous rétrograder vous-même.");
                return;
            }

            if (targetRank == null || targetRank == CityRank.MEMBER) {
                MessageUtil.sendMessage(sender, plugin.getPrefixError(), "&cCe joueur ne peut pas être rétrogradé davantage.");
                return;
            }

            boolean success = plugin.getCityManager().setMember(city, targetUUID, CityRank.MEMBER);
            if (success) {
                MessageUtil.sendMessage(sender, plugin.getPrefixInfo(), "&bLe joueur &d" + target.getName() + " &ba été rétrogradé au rang &dMembre&b.");
                MessageUtil.sendMessage(target, plugin.getPrefixError(), "&cVous avez été rétrogradé au rang &fMembre &cdans la ville &d" + city + "&c.");
            } else {
                MessageUtil.sendMessage(sender, plugin.getPrefixError(), "&cErreur lors de la rétrogradation.");
            }
        }
    }
}
