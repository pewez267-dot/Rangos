# ClaimBlocks Bug Fixes - Patch Instructions

## Quick Reference: Exact Changes Needed

### File 1: CBEventHandler.java

**Location 1 - After line with `stackInHand.method_7934(1);`**

FIND (around line 285-290):
```java
if (!((class_3222)player).method_7337()) {
    stackInHand.method_7934(1);
}
return class_1269.field_5814;
```

REPLACE WITH:
```java
if (!((class_3222)player).method_7337()) {
    stackInHand.method_7934(1);
    if (stackInHand.method_7960()) {
        ((class_3222)player).method_6030(hand, class_1799.field_8037);
    } else {
        ((class_3222)player).method_6030(hand, stackInHand);
    }
}
return class_1269.field_5812;
```

**Location 2-8 - All other returns in register$lambda$0**

FIND ALL occurrences of:
```java
return class_1269.field_5814;
```

REPLACE WITH:
```java
return class_1269.field_5812;
```

EXCEPT the first two returns which are `field_5811` (PASS) - leave those unchanged.

### File 2: CBManager.java

**Location: Inside isOverlapping method, after getName() check**

FIND (around line 271-275):
```java
if (Intrinsics.areEqual((Object)existingRegion.getName(), (Object)newRegion.getName())) {
    bl2 = false;
} else if (!(Intrinsics.areEqual((Object)existingRegion.getWorld(), (Object)"*") || ...
```

REPLACE WITH:
```java
if (Intrinsics.areEqual((Object)existingRegion.getName(), (Object)newRegion.getName())) {
    bl2 = false;
} else if (Intrinsics.areEqual((Object)existingRegion.getOwner(), (Object)newRegion.getOwner())) {
    bl2 = false;
} else if (!(Intrinsics.areEqual((Object)existingRegion.getWorld(), (Object)"*") || ...
```

## Summary
- CBEventHandler: 8-9 changes total (1 item sync fix + 7-8 ActionResult changes)
- CBManager: 1 change (owner comparison)
- Total: ~10 lines of code modified
