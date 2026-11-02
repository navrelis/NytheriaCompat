// TRACKED HASH: f1805acf0b005c3b2a599b6df75b8c44075821be
/*
 * Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md.
 * Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1.
 */

package dev.nytheria.compat.injects.server.packs.repository;

import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.RepositorySource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import dev.nytheria.compat.injections.server.packs.repository.PackRepositoryInjection;

import java.util.Set;

@Mixin(PackRepository.class)
public class PackRepositoryInject implements PackRepositoryInjection {
    @Shadow @Final @Mutable
    private Set<RepositorySource> sources;

    @Override
    public synchronized void addPackFinder(RepositorySource packFinder) {
        this.sources.add(packFinder);
    }
}