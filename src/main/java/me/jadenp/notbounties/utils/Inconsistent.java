package me.jadenp.notbounties.utils;

import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This describes data that could be changed elsewhere unknowingly.
 * To keep consistency when connecting to databases, data with equivalent IDs will have their sub elements merged.
 * All important data in an Inconsistent object should be stored with the 'final' modifier.
 * If data needs to be changed, it should be added or removed from the sub elements.
 */
public interface Inconsistent {
    /**
     * Get an identifier for this object.
     * Any objects with the same identifier should be considered the same object, but each in a different timeline.
     * @return An identifying string.
     */
    String getID();

    /**
     * Copies the object.
     * @return A new object identical to this one.
     */
    <T extends Inconsistent> T copy();

    /**
     * Get the latest major update of the object.
     * A "major update" could be the most important feature changing, or anything changing at all.
     * It just depends on the implementation.
     * @return The time in milliseconds.
     */
    long getLatestUpdate();

    /**
     * Get any sub elements that may be inconsistent.
     * @return A list of inconsistent sub elements holding data for this object.
     */
    List<Inconsistent> getSubElements();

    /**
     * Set the inconsistent sub elements.
     * @param subElements New elements.
     */
    void setSubElements(List<Inconsistent> subElements);

    /**
     * Searches for an item starting at the specified index.
     * @param identifier Identifier of the item to search for
     * @param items List of items to search through.
     * @param index Center of the search.
     * @return Index of the item that was found, or -1 if no item was found
     */
    private static int indexSearch(String identifier, List<? extends Inconsistent> items, int index) {
        if (items.isEmpty())
            return -1;
        if (index >= items.size())
            index = items.size() - 1;
        if (index < 0)
            index = 0;
        if (items.get(index).getID().equals(identifier))
            return index;
        for (int i = 1; i < items.size(); i++) {
            if (index + i < items.size() && items.get(index + i).getID().equals(identifier))
                    return index + i;

            if (index - i > 0 && items.get(index - i).getID().equals(identifier))
                    return index - 1;
            if (index + i >= items.size() && index - i < 0)
                break;
        }
        return -1;
    }

    /**
     * Compares 2 lists of inconsistent data.
     * If an item exists in one list but not the other, it will only be kept if the last update was after the last sync.
     * If items share the same identifier, the item with the latest update will be used as a base item,
     * and the 2 items' sub elements will be merged recursively.
     * Using lists in the same order will be faster.
     * @param list1 First list of inconsistent elements.
     * @param list2 Second list of inconsistent elements.
     * @param lastSync The time in milliseconds when the items were synced last.
     * @return A combined list of the most consistent data.
     * @param <T> An inconsistent object.
     */
    static <T extends Inconsistent> List<T> compareInconsistentLists(List<T> list1, List<T> list2, long lastSync) {
        List<T> consistentList = new ArrayList<>();
        int searchIndex = 0;
        for (int i = 0; i < list1.size(); i++) {
            T item1 = list1.get(i);
            // search for item1 in list2
            searchIndex = indexSearch(item1.getID(), list2, searchIndex == -1 ? i : searchIndex); // searchIndex should have the index of the last item in list2, or -1 if the last item didn't exist in list2
            if (searchIndex == -1) {
                // item1 doesn't exist in list2
                if (item1.getLatestUpdate() > lastSync) {
                    // item1 was added in list2 after last sync
                    consistentList.add(item1);
                }
            } else {
                // items exist in both lists
                T item2 = list2.remove(searchIndex); // item is removed to know that it has a match
                // set the base item to the one with the latest update
                T baseItem = item1.getLatestUpdate() > item2.getLatestUpdate() ? item1 : item2;
                // compare sub elements with recursion and set to baseItem's sub elements to make baseItem the consistent item
                baseItem.setSubElements(compareInconsistentLists(item1.getSubElements(), item2.getSubElements(), lastSync));
                consistentList.add(baseItem);
            }
        }

        // any items left in list2 do not exist in list1
        for (T item2 : list2) {
            if (item2.getLatestUpdate() > lastSync) {
                // item2 was added after last sync
                consistentList.add(item2);
            }
        }

        return consistentList;
    }

    /**
     * Get the asyncronous objects from 2 lists of inconsistent data.
     * The returned data is sorted into 2 categories per list.
     * The first is data that was added to the list since last sync.
     * The second is data that was removed from the  list since the last sync.
     * Objects will not appear in multiple lists.
     * @param list1 First list of inconsistent elements.
     * @param list2 Second list of inconsistent elements.
     * @param lastSync The time in milliseconds when the items were synced last.
     * @return An array of length 4 containing lists exclusively the data that isn't synced across the lists.
     * The array contains these categories of inconsistent data changed since the last sync:
     * [list2 objects added][list2 objects removed][list1 objects added][list1 objects removed]
     * @param <T> An inconsistent object.
     */
    @SuppressWarnings("unchecked")
    static <T extends Inconsistent> List<T>[] getAsyncronousObjects(List<T> list1, List<T> list2, long lastSync) {
        Bukkit.getLogger().info("last sync: " + lastSync);
        ArrayList<T> list2Added = new ArrayList<>();
        ArrayList<T> list2Removed = new ArrayList<>();
        ArrayList<T> list1Added = new ArrayList<>();
        ArrayList<T> list1Removed = new ArrayList<>();
        int searchIndex = 0;
        for (int i = 0; i < list1.size(); i++) {
            T item1 = list1.get(i);
            Bukkit.getLogger().info("i1: " + item1.getID() + " " + item1.getLatestUpdate());
            // search for item1 in list2
            searchIndex = indexSearch(item1.getID(), list2, searchIndex == -1 ? i : searchIndex); // searchIndex should have the index of the last item in list2, or -1 if the last item didn't exist in list2
            if (searchIndex == -1) {
                Bukkit.getLogger().info("no match");
                // item1 doesn't exist in list2
                if (item1.getLatestUpdate() > lastSync) {
                    // item1 was added to list1 after last sync
                    list1Added.add(item1);
                } else {
                    // item1 was removed from list2 after last sync
                    list2Removed.add(item1);
                }
            } else {
                // items exist in both lists
                T item2 = list2.remove(searchIndex); // item is removed to know that it has a match
                // update the inconsistent sub elements
                List<Inconsistent>[] inconsistentSubData = getAsyncronousObjects(item1.getSubElements(), item2.getSubElements(), lastSync);
                // add the added and removed versions of item1 to list2 lists
                if (!inconsistentSubData[0].isEmpty()) {
                    item1 = item1.copy();
                    item1.setSubElements(inconsistentSubData[0]);
                    list2Added.add(item1);
                }
                if (!inconsistentSubData[1].isEmpty()) {
                    item1 = item1.copy();
                    item1.setSubElements(inconsistentSubData[1]);
                    list2Removed.add(item1);
                }
                // add the added and removed versions of item2 to list1 lists
                if (!inconsistentSubData[2].isEmpty()) {
                    item2 = item2.copy();
                    item2.setSubElements(inconsistentSubData[2]);
                    list1Added.add(item2);
                }
                if (!inconsistentSubData[3].isEmpty()) {
                    item2 = item2.copy();
                    item2.setSubElements(inconsistentSubData[3]);
                    list1Removed.add(item2);
                }
            }
        }

        // any items left in list2 do not exist in list1
        for (T item2 : list2) {
            Bukkit.getLogger().info("i2: " + item2.getID() + " " + item2.getLatestUpdate());
            if (item2.getLatestUpdate() > lastSync) {
                // item2 was added to list2 after the last update
                list2Added.add(item2);
            } else {
                // item2 was removed from list1 after the last update
                list1Removed.add(item2);
            }
        }
        return new List[]{list2Added, list2Removed, list1Added, list1Removed};
    }
}
