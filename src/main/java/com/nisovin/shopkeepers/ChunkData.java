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
		World world = Bukkit.getServer().getWorld(this.worldName);
		if (world != null) {
			return world.isChunkLoaded(this.chunkX, this.chunkZ);
		}
		return false;
	}

	@Override
	public String toString() {
		return getClass().getName()
				+ "[worldName=" + this.worldName
				+ ",chunkX=" + this.chunkX
				+ ",chunkZ=" + this.chunkZ + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + this.chunkX;
		result = prime * result + this.chunkZ;
		result = prime * result + this.worldName.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (this.getClass() != obj.getClass()) return false;
		ChunkData other = (ChunkData) obj;
		if (this.chunkX != other.chunkX) return false;
		if (this.chunkZ != other.chunkZ) return false;
		if (!this.worldName.equals(other.worldName)) return false;
		return true;
	}
}