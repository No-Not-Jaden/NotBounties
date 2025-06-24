package me.jadenp.notbounties.features.settings.databases;


public interface TempDatabase {
   /**
    * Check if the database has this server's data.
    * @return True if this server has synced with the database before.
    */
   boolean hasServerData();

   /**
    * Add this server's data to the database.
    * This is just a note attached to the database, so {@link #hasServerData()} will return true next call.
    */
   void addServerData();

   /**
    * Check if the database is empty / has no server data.
    * @return True if the database is empty.
    */
   boolean isEmpty();

   /**
    * Check if the database has global data.
    * @return True if the database has global data.
    */
   boolean isGlobal();

   /**
    * Mark that this database has global data.
    */
   void setGlobal();
}
