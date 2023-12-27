package com.negativeonehero.inventorymod.mixin;

import com.negativeonehero.inventorymod.SortingType;
import com.negativeonehero.inventorymod.impl.IPlayerInventory;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin extends Screen {
    @Unique
    private PlayerInventory inventory;
    @Unique
    private ButtonWidget previousButton;
    @Unique
    private ButtonWidget nextButton;
    @Unique
    private ButtonWidget functionButton;
    @Unique
    private ButtonWidget sortingTypeButton;
    @Unique
    private boolean sorting = false;
    @Unique
    private int page = 1;
    @Unique
    private SortingType sortingType = SortingType.COUNT;

    // method_30163() = of() in 1.16.2+ mappings
    @Unique
    private Text previousTooltip = Text.method_30163("");
    @Unique
    private Text nextTooltip = Text.method_30163("");

    protected HandledScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "<init>", at = @At(value = "TAIL"))
    public void constructor(ScreenHandler handler, PlayerInventory inventory, Text title, CallbackInfo ci) {
        this.inventory = inventory;
    }

    @Inject(method = "init", at = @At(value = "TAIL"))
    public void init(CallbackInfo ci) {
        this.previousButton = new ButtonWidget(10, 10, 16, 16, Text.method_30163("<"),
                button -> this.update(false), (button, matrices, mouseX, mouseY) -> {
            if (mouseX >= button.x && mouseX <= button.x+button.getWidth()
                    && mouseY >= button.y && mouseY <= button.y+button.getHeight())
                this.renderTooltip(matrices, this.previousTooltip, mouseX, mouseY);
                });
        this.children.add(this.previousButton);
        this.nextButton = new ButtonWidget(86, 10, 16, 16, Text.method_30163(">"),
                button -> this.update(true), (button, matrices, mouseX, mouseY) -> {
            if (mouseX >= button.x && mouseX <= button.x+button.getWidth()
                    && mouseY >= button.y && mouseY <= button.y+button.getHeight())
                this.renderTooltip(matrices, this.nextTooltip, mouseX, mouseY);
        });
        this.children.add(this.nextButton);
        this.functionButton = new ButtonWidget(26, 10, 60, 16, Text.method_30163("Page " + page),
                button -> {
                    this.sorting = !this.sorting;
                    this.updateTooltip();
                });
        this.children.add(this.functionButton);
        this.sortingTypeButton = new ButtonWidget(10, 26, 92, 16, this.sortingType.message,
                button -> {
                    this.sortingType = this.sortingType.next();
                    button.setMessage(this.sortingType.message);
                });
        this.children.add(this.sortingTypeButton);
        this.addButton(this.previousButton);
        this.addButton(this.functionButton);
        this.addButton(this.nextButton);
        this.addButton(this.sortingTypeButton);
        this.updateTooltip();
    }

    @Unique
    private void update(boolean next) {
        if (sorting) {
            this.sort(next);
        } else {
            if(next) {
                if (this.page > 1) this.swapInventory(this.page);
                this.page++;
                this.previousButton.visible = true;
                if (this.page >= this.inventory.size() / 27) this.nextButton.visible = false;
                this.swapInventory(this.page);
            } else {
                this.swapInventory(this.page);
                this.page--;
                if (this.page < this.inventory.size() / 27) {
                    this.nextButton.visible = true;
                    if (this.page <= 1) this.previousButton.visible = false;
                    else this.swapInventory(this.page);
                }
            }
            this.updateTooltip();
        }
    }

    @Unique
    private void updateTooltip() {
        if(sorting) {
            this.previousTooltip = Text.method_30163("Sort Descending");
            this.nextTooltip = Text.method_30163("Sort Ascending");
            this.functionButton.setMessage(Text.method_30163("Sorting"));
        } else {
            this.previousTooltip = Text.method_30163("Page " + (page - 1));
            this.nextTooltip = Text.method_30163("Page " + (page + 1));
            this.functionButton.setMessage(Text.method_30163("Page " + page));
        }
    }

    @Unique
    public void sort(boolean ascending) {
        this.swapInventory(this.page);
        ArrayList<ItemStack> stacks = new ArrayList<>();
        int emptySlots = 0;
        for(int i = 9; i < this.inventory.main.size(); i++) {
            ItemStack stack = this.inventory.main.get(i);
            if (stack.isEmpty()) emptySlots++;
            else stacks.add(stack);
        }
        this.sortingType.sort(stacks, ascending);
        for(int i = 0; i < emptySlots; i++) {
            stacks.add(ItemStack.EMPTY);
        }
        for(int i = 0; i < stacks.size(); i++) {
            this.inventory.setStack(i + (i > 26 ? 14 : 9), stacks.get(i));
        }
        ((IPlayerInventory) this.inventory).setContentChanged();
        this.swapInventory(this.page);
    }

    @SuppressWarnings("ConstantValue")
    @Unique
    private void updateButtons() {
        boolean visible = (((Object) this) instanceof CreativeInventoryScreen
                && CreativeInventoryScreen.selectedTab == ItemGroup.INVENTORY.getIndex())
                || ((Object) this) instanceof InventoryScreen;
        if(sorting) {
            this.previousButton.visible = visible;
            this.functionButton.visible = visible;
            this.nextButton.visible = visible;
            this.sortingTypeButton.visible = visible;
        } else {
            this.previousButton.visible = this.page > 1 && visible;
            this.functionButton.visible = visible;
            this.nextButton.visible = this.page < this.inventory.size() / 27 && visible;
            this.sortingTypeButton.visible = false;
        }
    }

    @Inject(method = "removed", at = @At(value = "HEAD"))
    public void resetInventory(CallbackInfo ci) {
        this.swapInventory(this.page);
    }

    @Inject(method = "render", at = @At(value = "HEAD"))
    public void updateButtonsVisibility(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        this.updateButtons();
    }

    @Inject(method = "tick", at = @At(value = "HEAD"))
    public void tickButtons(CallbackInfo ci) {
        if(!this.sorting && !this.nextButton.visible && 27 * page + 9 < this.inventory.main.size()) {
            this.nextButton.visible = true;
        }
    }

    @Unique
    public void swapInventory(int page) {
        if(page > 1) {
            int startIndex = 27 * (page - 1) + 9;
            ArrayList<ItemStack> stack;
            stack = new ArrayList<>(this.inventory.main.subList(startIndex, startIndex + 27));
            for(int i = 0; i < 27; i++) {
                this.inventory.setStack(startIndex + i + 5, this.inventory.getStack(i + 9));
                this.inventory.setStack(i + 9, stack.get(i));
            }
            ((IPlayerInventory) this.inventory).setContentChanged();
        }
    }
}
