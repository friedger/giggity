package net.gaast.deoxide;

import java.util.HashMap;

import android.app.Application;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.util.Log;

public class DeoxideDb {
	Helper dbh;
	
	public DeoxideDb(Application app_) {
		dbh = new Helper(app_, "deoxide0", null, 1);
	}
	
	public Connection getConnection() {
		Log.i("DeoxideDb", "Created database connection");
		return new Connection();
	}
	
	public class Helper extends SQLiteOpenHelper {
		public Helper(Context context, String name, CursorFactory factory,
				int version) {
			super(context, name, factory, version);
		}
	
		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("Create Table schedule (sch_id Integer Primary Key AutoIncrement Not Null," +
					                          "sch_id_s VarChar(128))");
			db.execSQL("Create Table schedule_item (sci_id Integer Primary Key AutoIncrement Not Null," +
					                               "sci_sch_id Integer Not Null," +
					                               "sci_id_s VarChar(128)," +
					                               "sci_remind Boolean," +
					                               "sci_stars Integer(2) Null)");
		}
	
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// TODO Auto-generated method stub
	
		}
	}
	
	public class Connection {
		private SQLiteDatabase db;
		private Schedule sched;

		private HashMap<String,Long> sciIdMap;
		private long schId;
		
		public Connection() {
			db = dbh.getWritableDatabase();
		}
		
		protected void finalize() {
			db.close();
		}
		
		public void setSchedule(Schedule sched_) {
			Cursor q;
			
			sched = sched_;
			sciIdMap = new HashMap<String,Long>();
			
			Log.d("bla", "" + sched.getId());
			q = db.rawQuery("Select sch_id From schedule Where sch_id_s = ?",
					        new String[]{sched.getId()});
			
			if (q.getCount() == 0) {
				/*
				q = db.rawQuery("Insert Into schedule (sch_id_s) Values (?)",
						    new String[]{sched.getId()});
				TODO(wilmer): Sanitize this code after figuring out WTF
				lastInsertRow() is only available via this kiddie
				interface:
				*/
				
				ContentValues row = new ContentValues();
				
				row.put("sch_id_s", sched.getId());
				schId = db.insert("schedule", null, row);
			} else if (q.getCount() == 1) {
				q.moveToNext();
				schId = q.getLong(0);
			} else {
				Log.e("DeoxideDb", "Database corrupted");
			}
			Log.i("DeoxideDb", "schedId: " + schId);
			q.close();
			
			q = db.rawQuery("Select sci_id, sci_id_s, sci_remind, sci_stars " +
					        "From schedule_item Where sci_sch_id = ?",
					        new String[]{"" + schId});
			while (q.moveToNext()) {
				Schedule.Item item = sched.getItem(q.getString(1));
				item.setRemind(q.getInt(2) != 0);
				sciIdMap.put(q.getString(1), new Long(q.getInt(0)));
				Log.d("sci", q.toString());
			}
			q.close();
		}
		
		public void saveScheduleItem(Schedule.Item item) {
			ContentValues row = new ContentValues();
			Long sciId;
			
			row.put("sci_sch_id", schId);
			row.put("sci_id_s", item.getId());
			row.put("sci_remind", item.getRemind());
			
			if ((sciId = sciIdMap.get(item.getId())) != null) {
				db.update("schedule_item", row,
						  "sci_id = ?", new String[]{"" + sciId.longValue()});
			} else {
				sciIdMap.put(item.getId(),
						     new Long(db.insert("schedule_item", null, row)));
			}
		}
	}
}