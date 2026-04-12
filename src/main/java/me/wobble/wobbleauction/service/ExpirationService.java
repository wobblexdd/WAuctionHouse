package me.wobble.wobbleauction.service;

import me.wobble.wobbleauction.WobbleAuction;
import me.wobble.wobbleauction.model.AuctionListing;
import me.wobble.wobbleauction.repository.AuctionRepository;
import me.wobble.wobbleauction.repository.ExpiredRepository;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

public final class ExpirationService {

    private final WobbleAuction plugin;
    private final AuctionRepository auctionRepository;
    private final ExpiredRepository expiredRepository;
    private BukkitTask task;

    public ExpirationService(WobbleAuction plugin,
                             AuctionRepository auctionRepository,
                             ExpiredRepository expiredRepository) {
        this.plugin = plugin;
        this.auctionRepository = auctionRepository;
        this.expiredRepository = expiredRepository;
    }

    public void start() {
        long intervalSeconds = plugin.getConfig().getLong("auction.check-interval-seconds", 60L);
        long intervalTicks = Math.max(20L, intervalSeconds * 20L);

        this.task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            List<AuctionListing> expired = auctionRepository.findExpiredActive(now);

            for (AuctionListing listing : expired) {
                boolean marked = auctionRepository.expireListingIfActive(listing.getListingId());
                if (marked) {
                    expiredRepository.add(listing.getSellerId(), listing.getItem(), now);
                }
            }
        }, intervalTicks, intervalTicks);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
    }
}
