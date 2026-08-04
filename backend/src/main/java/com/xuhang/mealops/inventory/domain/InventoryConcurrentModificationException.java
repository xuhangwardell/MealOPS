package com.xuhang.mealops.inventory.domain;
public class InventoryConcurrentModificationException extends RuntimeException { public InventoryConcurrentModificationException(){super("Inventory changed concurrently");} }
