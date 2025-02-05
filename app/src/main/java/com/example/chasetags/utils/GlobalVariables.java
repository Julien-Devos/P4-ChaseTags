package com.example.chasetags.utils;

public class GlobalVariables {

    /**
     *
     * Inter-activity communication EXTRA tags
     * USAGE: Used for Intent Extras
     *
     * EXN = "Extra name" = the String name of the added Extra
     * EXV = "Extra value" = the value of the extra
     *
     */

    public static final String EXN_FROM = "EXTRA_FROM";
    public static final int EXV_FROM_GAME = 0;

    public static final String EXN_Owner_ID = "ownerID";
    public static final String EXN_Rune_ID = "runeID";

    public static final String EXN_Room_code = "roomCode";

    public static final String EXN_Runes_total = "runesTotal";
    public static final String EXN_Player_role = "playerRole";

    public static final String EXN_rune_nav_ID = "itemIdSelected";
    public static final String EXN_EndGame_Reason = "endGameReason";


    /**
     *
     * Inter-activity communication Shared preferences tag
     * USAGE: Used to get Shared preferences Items
     *
     */

    public static final String PREFS_preference_name = "PREFS";
    public static final String PREFS_player_name = "playerName";
    public static final String PREFS_rune_progress = "runeProgress";
    public static final String PREFS_runes_validated_list = "runesValidatedList";

    public static final String PREFS_preference_dialogs = "dialogs";
    public static final String PREFS_showDialog_local = "localRune";
    public static final String PREFS_showDialog_global = "globalRune";
    public static final String PREFS_showDialog_editGameRune = "editGameRune";

    public static final String PREFS_preference_tutorial = "dialogs";
    public static final String PREFS_showTutorial = "showTutorial";


    /**
     *
     * Inter-activity communication Database tags
     * USAGE: Names of database calls elements
     *
     */

    public static final String Firestore_Collection_users = "users";
    public static final String Firestore_Collection_runes = "runes";
    public static final String Firestore_User_ownerUID = "ownerUID";
    public static final String Firestore_Document_Rune_name = "name";
    public static final String Firestore_Document_Rune_value = "value";
    public static final String Firestore_Document_Rune_score = "score";
    public static final String Firestore_Document_Rune_localisationClue = "localisationClue";

    public static final String RealtimeDB_rooms = "rooms";
    public static final String RealtimeDB_players = "players";
    public static final String RealtimeDB_player_host = "host";
    public static final String RealtimeDB_player_guest = "guest";
    public static final String RealtimeDB_gameStarted = "gameStarted";
    public static final String RealtimeDB_Room_Runes_list = "roomRunesList";
    public static final String RealtimeDB_endGameReason = "dbEndGameReason";


    /**
     *
     * Inter-activity Rune type items
     * USAGE: Used to define a Rune object Type
     *
     */

    public static final String Rune_type = "type";
    public static final String Rune_code_object_type = "code";
    public static final String Rune_qr_object_type = "qr";
    public static final String Rune_nfc_object_type = "nfc";

    public static final String Rune_name = "name";
    public static final String Rune_hint = "loc";
    public static final String Rune_code = "code";
    public static final String Rune_score = "score";

    public static final String RealtimeDB_timerSelection = "gameTimer";


    /**
     *
     * Inter-activity game settings items
     * USAGE: Used to define game settings
     *
     */

    public static final String gameSettings_timer_status = "timer";
    public static final int gameSettings_timer_disabled = 0;
    public static final int gameSettings_timer_enabled = 1;

    public static final String gameSettings_timer_hours = "hours";
    public static final String gameSettings_timer_minutes = "minutes";

    public static final String gameSettings_ending_choice = "gameEnd";
    public static final int gameSettings_ending_timerEnd = 0;
    public static final int gameSettings_ending_allPlayers = 1;
    public static final int gameSettings_ending_firstPlayer = 2;
    public static final int gameSettings_ending_3firstPlayers = 3;

    public static final String gameSettings_mode = "gameMode";
    public static final String gameSettings_hints = "hints";
    public static final int gameSettings_hints_rune_chase = 0;
    public static final int gameSettings_hints_tracking_game = 1;


    /**
     *
     * Inter-activity tags items
     * USAGE: Used to define tags
     *
     */

    public static final String tag_deleteRune_dialog = "delete_rune_dialog";
    public static final String tag_editRune_dialog = "edit_rune_dialog";


    /*
     * Player keys for the database
     */

    public static final String RealtimeDB_player_name = "name";
    public static final String RealtimeDB_player_role = "role";
    public static final String RealtimeDB_player_runesCollected = "runesCollected";
    public static final String RealtimeDB_player_runesLeft = "runesLeft";
    public static final String RealtimeDB_player_score = "score";

}
