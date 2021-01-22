package cy.jdkdigital.productivebees.common.tileentity;

import cy.jdkdigital.productivebees.init.ModFluids;
import cy.jdkdigital.productivebees.init.ModItems;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.CapabilityItemHandler;

public abstract class FluidTankTileEntity extends CapabilityTileEntity implements ITickableTileEntity
{
    private int tankTick = 0;

    public FluidTankTileEntity(TileEntityType<?> type) {
        super(type);
    }

    @Override
    public void tick() {
        if (world != null && !world.isRemote) {
            if (++this.tankTick > 20) {
                this.tankTick = 0;
                tickFluidTank();
            }
        }
    }

    public void tickFluidTank() {
        this.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY).ifPresent(fluidHandler -> {
            FluidStack fluidStack = fluidHandler.getFluidInTank(0);
            if (fluidStack.getAmount() >= 0) {
                this.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).ifPresent(invHandler -> {
                    ItemStack fluidContainerItem = invHandler.getStackInSlot(InventoryHandlerHelper.BOTTLE_SLOT);
                    ItemStack existingOutput = invHandler.getStackInSlot(InventoryHandlerHelper.FLUID_ITEM_OUTPUT_SLOT);
                    if (fluidContainerItem.getCount() > 0 && (existingOutput.isEmpty() || (existingOutput.getCount() < existingOutput.getMaxStackSize()))) {
                        ItemStack outputItem = null;
                        if (fluidContainerItem.getItem() == Items.GLASS_BOTTLE && fluidStack.getAmount() >= 250 && fluidStack.getFluid().isEquivalentTo(ModFluids.HONEY.get())) {
                            outputItem = new ItemStack(Items.HONEY_BOTTLE);
                        } else if (fluidContainerItem.getItem() == Items.HONEYCOMB && fluidStack.getAmount() >= 250 && fluidStack.getFluid().isEquivalentTo(ModFluids.HONEY.get())) {
                            outputItem = new ItemStack(ModItems.HONEY_TREAT.get());
                        } else {
                            FluidActionResult fillResult = FluidUtil.tryFillContainer(fluidContainerItem, fluidHandler, Integer.MAX_VALUE, null, true);
                            if (fillResult.isSuccess()) {
                                outputItem = fillResult.getResult();
                            }
                        }

                        if (outputItem != null) {
                            if (invHandler.insertItem(InventoryHandlerHelper.FLUID_ITEM_OUTPUT_SLOT, outputItem, true).equals(ItemStack.EMPTY)) {
                                boolean bottleOutput = outputItem.getItem().equals(Items.HONEY_BOTTLE) || outputItem.getItem().equals(ModItems.HONEY_TREAT.get());
                                int drainedFluid = bottleOutput ? 250 : 0;

                                if (outputItem.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY).isPresent()) {
                                    drainedFluid = outputItem.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY).map(h -> h.getFluidInTank(0).getAmount()).orElse(0);
                                }
                                fluidHandler.drain(drainedFluid, IFluidHandler.FluidAction.EXECUTE);

                                // If item container is full or internal tank is empty, move the item to the output @TODO doesn't work
                                boolean doneFilling = outputItem.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY).map(h -> h.getTankCapacity(0) > h.getFluidInTank(0).getAmount()).orElse(true);
                                if (bottleOutput || doneFilling) {
                                    fluidContainerItem.shrink(1);
                                    invHandler.insertItem(InventoryHandlerHelper.FLUID_ITEM_OUTPUT_SLOT, outputItem, false);
                                } else {
                                    invHandler.insertItem(InventoryHandlerHelper.BOTTLE_SLOT, outputItem, false);
                                }
                            }
                        }
                    }
                });
            }
        });
    }
}
