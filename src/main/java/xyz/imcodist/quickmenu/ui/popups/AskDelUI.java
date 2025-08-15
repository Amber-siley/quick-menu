package xyz.imcodist.quickmenu.ui.popups;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.*;
import io.wispforest.owo.ui.core.*;
import net.minecraft.text.Text;
import xyz.imcodist.quickmenu.data.ActionButtonData;
import xyz.imcodist.quickmenu.ui.surfaces.SwitcherSurface;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class AskDelUI extends OverlayContainer<FlowLayout> {
    public Consumer<ActionButtonData> onConfirmDelete;
    public Supplier<Integer> onCancelDelete;

    public AskDelUI(ActionButtonData actionData) {
        super(Containers.verticalFlow(Sizing.fill(100), Sizing.fill(100)));
        FlowLayout rootComponent = child;

        // Set up root.
        rootComponent
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER);

        // Set up the main layout.
        FlowLayout mainLayout = Containers.verticalFlow(Sizing.fixed(120), Sizing.fixed(70));
        mainLayout
                .surface(new SwitcherSurface())
                .padding(Insets.of(15));
        rootComponent.child(mainLayout);

        // Add warning text.
        LabelComponent warningLabel = Components.label(Text.translatable("menu.delete.warning"));
        warningLabel
                .horizontalTextAlignment(HorizontalAlignment.CENTER)
                .verticalTextAlignment(VerticalAlignment.CENTER)
                .horizontalSizing(Sizing.fill(105))
                .margins(Insets.of(0, 5, 0, 0));
        mainLayout.child(warningLabel);

        // Create button layout.
        FlowLayout buttonLayout = Containers.horizontalFlow(Sizing.fill(105), Sizing.content());
        buttonLayout.horizontalAlignment(HorizontalAlignment.CENTER);
        buttonLayout.gap(10);
        mainLayout.child(buttonLayout);

        // Add confirm delete button.
        ButtonComponent confirmButton = Components.button(Text.translatable("menu.delete.confirm_button"), (buttonComponent) -> {
            if (onConfirmDelete != null) {
                onConfirmDelete.accept(actionData);
            }
            remove();
        });
        confirmButton.margins(Insets.of(0));
        // Set custom colors for delete button
        buttonLayout.child(confirmButton);

        // Add cancel button.
        ButtonComponent cancelButton = Components.button(Text.translatable("menu.delete.cancel"), (buttonComponent) -> {
            if (onCancelDelete != null) {
                onCancelDelete.get();
            }
            remove();
        });
        cancelButton.margins(Insets.of(0));
        buttonLayout.child(cancelButton);
    }
}
