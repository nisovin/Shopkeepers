package com.nisovin.shopkeepers;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;

public final class ChunkData {

	public final String worldName;
	public final int chunkX;
	public final int chunkZ;

	public ChunkData(String worldName, int chunkX, int chunkZ) {
		Validate.notNull(worldName);
		this.worldName = worldName;
		this.chunkX = chunkX;
		this.chunkZ = chunkZ;
	}

	public ChunkData(Chunk chunk) {
		Validate.notNull(chunk);
		this.worldName = chunk.getWorld().getName();
		this.chunkX = chunk.getX();
		this.chunkZ = chunk.getZ();
	}

	public boolean isChunkLoaded() {
		World world = Bukkit.getServer().getWorld(worldName);
		if (world != null) {
			return world.isChunkLoaded(chunkX, chunkZ);
		}
		return false;
	}

	@Override
	public String toString() {
		return getClass().getName()
				+ "[worldName=" + worldName
				+ ",chunkX=" + chunkX
				+ ",chunkZ=" + chunkZ + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + chunkX;
		result = prime * result + chunkZ;
		result = prime * result + worldName.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (this.getClass() != obj.getClass()) return false;
		ChunkData other = (ChunkData) obj;
		if (chunkX != other.chunkX) return false;
		if (chunkZ != other.chunkZ) return false;
		if (!worldName.equals(other.worldName)) return false;
		return true;
	}
}