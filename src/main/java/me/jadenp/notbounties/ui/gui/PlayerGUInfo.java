package me.jadenp.notbounties.ui.gui;

import me.jadenp.notbounties.ui.gui.display_items.DisplayItem;

import java.util.List;

public record PlayerGUInfo(long page, String guiType, Object[] data, List<DisplayItem> displayItems, String title) {
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o instanceof PlayerGUInfo guInfo) {
            return guInfo.page == page && guInfo.guiType.equals(guiType) && guInfo.title.equals(title);
        }
        return false;
    }
}
