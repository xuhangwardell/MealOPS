package com.xuhang.mealops.inventory.domain;
import java.math.BigDecimal;
public record InventoryConsumptionAllocation(int position,long batchId,long expectedVersion,BigDecimal amount,BigDecimal before,BigDecimal after,String unit) {
 public InventoryConsumptionAllocation { if(position<=0||batchId<=0||expectedVersion<0||amount==null||before==null||after==null||unit==null) throw new InvalidInventoryBatchException("Invalid allocation"); var u=com.xuhang.mealops.measurement.domain.Unit.fromCode(unit).orElseThrow(()->new InvalidInventoryBatchException("Invalid allocation unit")); if(!u.isBaseUnit()||amount.signum()<=0||before.signum()<0||after.signum()<0||before.compareTo(amount.add(after))!=0) throw new InvalidInventoryBatchException("Invalid allocation values"); }
}
