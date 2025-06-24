package me.jadenp.notbounties.utils;


import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This describes data that could be changed elsewhere unknowingly.
 * To keep consistency when connecting to databases, data with equivalent IDs will have their sub elements merged.
 * All important data in an Inconsistent object should be stored with the 'final' modifier.
 * If data needs to be changed, it should be added or removed from the sub elements.
 */
public abstract class Inconsistent {

    private final Map<String, Long> lastSyncTimes = new ConcurrentHashMap<>();

    /**
     * Get an identifier for this object.
     * Any objects with the same identifier should be considered the same object, but each in a different timeline.
     * This is usually a UUID of a player. For subitems that shouldn't be individually modified, e.g., A setter object
     * for a bounty, final parameters and a timestamp should be used.
     * @return An identifying string.
     */
    public abstract String getID();

    /**
     * Copies the object.
     * @return A new object identical to this one.
     */
    public abstract <T extends Inconsistent> T copy();

    /**
     * Get the latest major update of the object.
     * A "major update" could be the most important feature changing, or anything changing at all.
     * It just depends on the implementation.
     * @return The time in milliseconds.
     */
    public abstract long getLatestUpdate();

    /**
     * Get any sub elements that may be inconsistent.
     * @return A list of inconsistent sub elements holding data for this object.
     */
    public abstract List<Inconsistent> getSubElements();

    /**
     * Set the inconsistent sub elements.
     * @param subElements New elements.
     */
    public abstract void setSubElements(List<Inconsistent> subElements);

    /**
     * Get the time when this object was individually synced with another data set.
     * This may not be the most up-to-date sync time.
     * @param syncName The name of the data set that this object synced with.
     * @return The sync time.
     */
    public long getLastSyncOverride(String syncName) {
        if (lastSyncTimes.containsKey(syncName))
            return lastSyncTimes.get(syncName);
        return -1;
    }

    /**
     * Set the time when this object was individually synced with another data set.
     * @param syncName The name of the data set that this object synced with.
     * @param time The sync time.
     */
    public void setLastSyncOverride(String syncName, long time) {
        lastSyncTimes.put(syncName, time);
        for (Inconsistent inconsistent : getSubElements()) {
            inconsistent.setLastSyncOverride(syncName, time);
        }
    }

    /**
     * Searches for an item starting at the specified index.
     * @param identifier Identifier of the item to search for
     * @param items List of items to search through.
     * @param index Center of the search.
     * @return Index of the item that was found, or -1 if no item was found
     */
    public static int indexSearch(String identifier, List<? extends Inconsistent> items, int index) {
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

            if (index - i >= 0 && items.get(index - i).getID().equals(identifier))
                    return index - i;
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
     * @param syncName The name of the data that is being synchronized.
     * @return A combined list of the most consistent data.
     * @param <T> An inconsistent object.
     */
    public static <T extends Inconsistent> List<T> compareInconsistentLists(List<T> list1, List<T> list2, long lastSync, String syncName, boolean includeUnchanged) {
        List<T> consistentList = new ArrayList<>();
        int searchIndex;
        int lastValidIndex = 0;
        for (T item1 : list1) {
            if (item1 == null)
                continue;
            // search for item1 in list2
            searchIndex = indexSearch(item1.getID(), list2, lastValidIndex);
            // get item 2 or null
            T item2 = searchIndex == -1 ? null : list2.remove(searchIndex);
            // compare items
            if (item1.equals(item2)) {
                if (includeUnchanged)
                    consistentList.add(item1.copy());
            } else {
                T consistentItem = compareInconsistentObjects(item1, item2, lastSync, syncName);
                if (consistentItem != null) {
                    consistentList.add(consistentItem);
                }
            }
            if (searchIndex != -1)
                lastValidIndex = searchIndex + 1;

        }

        // any items left in list2 do not exist in list1
        for (T item2 : list2) {
            long syncTime = Math.max(item2.getLastSyncOverride(syncName), lastSync);
            if (item2.getLatestUpdate() > syncTime) {
                // item2 was added after the last sync
                consistentList.add(item2);
            }
        }

        return consistentList;
    }

    /**
     * Get the asynchronous objects from 2 lists of inconsistent data.
     * The returned data is sorted into 2 categories per list.
     * The first is data added to the list since last sync.
     * The second is data removed from the list since the last sync.
     * Objects will not appear in multiple lists.
     * @param list1 First list of inconsistent elements.
     * @param list2 Second list of inconsistent elements.
     * @param lastSync The time in milliseconds when the items were synced last.
     * @param syncName The name of the data that is being synchronized.
     * @return An array of length 4 containing lists exclusively the data that isn't synced across the lists.
     * The array contains these categories of inconsistent data changed since the last sync:
     * [list2 objects added][list2 objects removed][list1 objects added][list1 objects removed]
     * @param <T> An inconsistent object.
     */
    @SuppressWarnings("unchecked")
    public static <T extends Inconsistent> List<T>[] getAsynchronousObjects(List<T> list1, List<T> list2, long lastSync, String syncName) {
        ArrayList<T> list2Added = new ArrayList<>();
        ArrayList<T> list2Removed = new ArrayList<>();
        ArrayList<T> list1Added = new ArrayList<>();
        ArrayList<T> list1Removed = new ArrayList<>();
        int searchIndex;
        int lastValidIndex = 0;
        for (int i = 0; i < list1.size(); i++) {

            T item1 = list1.get(i);
            // search for item1 in list2
            searchIndex = indexSearch(item1.getID(), list2, lastValidIndex); // searchIndex should have the index of the last item in list2, or -1 if the last item didn't exist in list2
            if (searchIndex == -1) {
                long syncTime = Math.max(item1.getLastSyncOverride(syncName), lastSync);
                // item1 doesn't exist in list2
                if (item1.getSubElements().isEmpty()) {
                    // BASE CASE - no sub elements
                    if (item1.getLatestUpdate() > syncTime) {
                        // item1 was added to list1 after the last sync
                        list1Added.add(item1);
                    } else {
                        // item1 was removed from list2 after the last sync
                        list2Removed.add(item1);
                    }
                } else {
                    // check sub-elements
                    List<Inconsistent>[] inconsistentSubData = getAsynchronousObjects(item1.getSubElements(), new ArrayList<>(), lastSync, syncName);
                    // add the removed versions of item1 to the list2 list
                    if (!inconsistentSubData[1].isEmpty()) {
                        item1 = item1.copy();
                        item1.setSubElements(inconsistentSubData[1]);
                        list2Removed.add(item1);
                    }
                    // add the added versions of item1 to the list1 list
                    if (!inconsistentSubData[2].isEmpty()) {
                        item1 = item1.copy();
                        item1.setSubElements(inconsistentSubData[2]);
                        list1Added.add(item1);
                    }
                }
            } else {
                // items exist in both lists
                T item2 = list2.remove(searchIndex); // item is removed to know that it has a match
                // update the inconsistent sub elements
                List<Inconsistent>[] inconsistentSubData = getAsynchronousObjects(item1.getSubElements(), item2.getSubElements(), lastSync, syncName);
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
            if (searchIndex != -1)
                lastValidIndex = searchIndex + 1;
        }

        // any items left in list2 do not exist in list1
        for (T item2 : list2) {
            long syncTime = Math.max(item2.getLastSyncOverride(syncName), lastSync);
            if (item2.getLatestUpdate() > syncTime) {
                // item2 was added to list2 after the last update
                list2Added.add(item2);
            } else {
                // item2 was removed from list1 after the last update
                list1Removed.add(item2);
            }
        }
        return new List[]{list2Added, list2Removed, list1Added, list1Removed};
    }

    /**
     * Compare data between 2 inconsistent objects. The latest data will be taken from each and combined.
     * The baseObj and comparingObj can be interchanged, and this method will produce the same result unless one of them
     * is null.
     *
     * @param baseObj A nonnull inconsistent element.
     * @param comparingObj A nullable inconsistent element. A null value represents a missing item, or that it was
     *                     removed.
     * @param lastSync The time when these objects were synced last.
     * @param syncName The name of the data that is being synchronized.
     * @return The combination of the 2 objects. Null if the entire object was removed since the last sync.
     * @param <T> An inconsistent object.
     */
    public static @Nullable <T extends Inconsistent> T compareInconsistentObjects(@NotNull T baseObj, @Nullable T comparingObj, long lastSync, String syncName) {
        if (comparingObj == null) {
            // comparing object is missing / was removed
            long syncTime = Math.max(baseObj.getLastSyncOverride(syncName), lastSync);
            if (baseObj.getLatestUpdate() > syncTime) {
                // the base object was updated after the last sync
                if (baseObj.getSubElements().isEmpty()) {
                    // no sub elements in the list
                    return baseObj.copy();
                }
                // get consistent sub elements

                List<Inconsistent> subElements = compareInconsistentLists(baseObj.getSubElements(), new ArrayList<>(), syncTime, syncName, true);
                T baseItem = baseObj.copy();
                baseItem.setSubElements(subElements);
                return baseItem;
            } else {
                // the base object has been removed since the last sync, and there have been no updates
                return null;
            }
        } else {
            // both objects exist
            // set the base item to the one with the latest update
            T baseItem = baseObj.getLatestUpdate() > comparingObj.getLatestUpdate() ? baseObj.copy() : comparingObj.copy();
            long syncTime = Math.max(comparingObj.getLastSyncOverride(syncName), Math.max(baseItem.getLastSyncOverride(syncName), lastSync));
            // compare sub elements and set to baseItem's sub elements to make baseItem the consistent item
            baseItem.setSubElements(compareInconsistentLists(baseObj.getSubElements(), new ArrayList<>(comparingObj.getSubElements()), syncTime, syncName, true));
            return baseItem;
        }
    }

    /**
     * Adds elements from the old list to the new list if their last update was before time.
     * @param oldList List of elements with updates before the time variable.
     * @param newList New list of elements with missing data.
     * @param time The threshold for elements.
     */
    public static <T extends Inconsistent> void syncDataBefore(List<T> oldList, List<T> newList, long time) {
        for (T item : oldList ) {
            if (item.getLatestUpdate() < time && !newList.contains(item) ) {
                newList.add(item);
            }
        }
    }
}
