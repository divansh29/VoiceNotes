package com.voicenotes.app.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.IllegalStateException;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class VoiceNoteDao_Impl implements VoiceNoteDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<VoiceNote> __insertionAdapterOfVoiceNote;

  private final Converters __converters = new Converters();

  private final EntityDeletionOrUpdateAdapter<VoiceNote> __deletionAdapterOfVoiceNote;

  private final EntityDeletionOrUpdateAdapter<VoiceNote> __updateAdapterOfVoiceNote;

  private final SharedSQLiteStatement __preparedStmtOfDeleteVoiceNoteById;

  public VoiceNoteDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfVoiceNote = new EntityInsertionAdapter<VoiceNote>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `voice_notes` (`id`,`title`,`filePath`,`duration`,`fileSize`,`createdAt`,`transcript`,`summary`,`keyPoints`,`isProcessing`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final VoiceNote entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getTitle());
        statement.bindString(3, entity.getFilePath());
        statement.bindLong(4, entity.getDuration());
        statement.bindLong(5, entity.getFileSize());
        final Long _tmp = __converters.dateToTimestamp(entity.getCreatedAt());
        if (_tmp == null) {
          statement.bindNull(6);
        } else {
          statement.bindLong(6, _tmp);
        }
        if (entity.getTranscript() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getTranscript());
        }
        if (entity.getSummary() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getSummary());
        }
        final String _tmp_1 = __converters.fromStringList(entity.getKeyPoints());
        statement.bindString(9, _tmp_1);
        final int _tmp_2 = entity.isProcessing() ? 1 : 0;
        statement.bindLong(10, _tmp_2);
      }
    };
    this.__deletionAdapterOfVoiceNote = new EntityDeletionOrUpdateAdapter<VoiceNote>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `voice_notes` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final VoiceNote entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfVoiceNote = new EntityDeletionOrUpdateAdapter<VoiceNote>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `voice_notes` SET `id` = ?,`title` = ?,`filePath` = ?,`duration` = ?,`fileSize` = ?,`createdAt` = ?,`transcript` = ?,`summary` = ?,`keyPoints` = ?,`isProcessing` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final VoiceNote entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getTitle());
        statement.bindString(3, entity.getFilePath());
        statement.bindLong(4, entity.getDuration());
        statement.bindLong(5, entity.getFileSize());
        final Long _tmp = __converters.dateToTimestamp(entity.getCreatedAt());
        if (_tmp == null) {
          statement.bindNull(6);
        } else {
          statement.bindLong(6, _tmp);
        }
        if (entity.getTranscript() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getTranscript());
        }
        if (entity.getSummary() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getSummary());
        }
        final String _tmp_1 = __converters.fromStringList(entity.getKeyPoints());
        statement.bindString(9, _tmp_1);
        final int _tmp_2 = entity.isProcessing() ? 1 : 0;
        statement.bindLong(10, _tmp_2);
        statement.bindLong(11, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteVoiceNoteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM voice_notes WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertVoiceNote(final VoiceNote voiceNote,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfVoiceNote.insertAndReturnId(voiceNote);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteVoiceNote(final VoiceNote voiceNote,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfVoiceNote.handle(voiceNote);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateVoiceNote(final VoiceNote voiceNote,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfVoiceNote.handle(voiceNote);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteVoiceNoteById(final long id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteVoiceNoteById.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteVoiceNoteById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<VoiceNote>> getAllVoiceNotes() {
    final String _sql = "SELECT * FROM voice_notes ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"voice_notes"}, new Callable<List<VoiceNote>>() {
      @Override
      @NonNull
      public List<VoiceNote> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfFilePath = CursorUtil.getColumnIndexOrThrow(_cursor, "filePath");
          final int _cursorIndexOfDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "duration");
          final int _cursorIndexOfFileSize = CursorUtil.getColumnIndexOrThrow(_cursor, "fileSize");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfTranscript = CursorUtil.getColumnIndexOrThrow(_cursor, "transcript");
          final int _cursorIndexOfSummary = CursorUtil.getColumnIndexOrThrow(_cursor, "summary");
          final int _cursorIndexOfKeyPoints = CursorUtil.getColumnIndexOrThrow(_cursor, "keyPoints");
          final int _cursorIndexOfIsProcessing = CursorUtil.getColumnIndexOrThrow(_cursor, "isProcessing");
          final List<VoiceNote> _result = new ArrayList<VoiceNote>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final VoiceNote _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpFilePath;
            _tmpFilePath = _cursor.getString(_cursorIndexOfFilePath);
            final long _tmpDuration;
            _tmpDuration = _cursor.getLong(_cursorIndexOfDuration);
            final long _tmpFileSize;
            _tmpFileSize = _cursor.getLong(_cursorIndexOfFileSize);
            final Date _tmpCreatedAt;
            final Long _tmp;
            if (_cursor.isNull(_cursorIndexOfCreatedAt)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getLong(_cursorIndexOfCreatedAt);
            }
            final Date _tmp_1 = __converters.fromTimestamp(_tmp);
            if (_tmp_1 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.util.Date', but it was NULL.");
            } else {
              _tmpCreatedAt = _tmp_1;
            }
            final String _tmpTranscript;
            if (_cursor.isNull(_cursorIndexOfTranscript)) {
              _tmpTranscript = null;
            } else {
              _tmpTranscript = _cursor.getString(_cursorIndexOfTranscript);
            }
            final String _tmpSummary;
            if (_cursor.isNull(_cursorIndexOfSummary)) {
              _tmpSummary = null;
            } else {
              _tmpSummary = _cursor.getString(_cursorIndexOfSummary);
            }
            final List<String> _tmpKeyPoints;
            final String _tmp_2;
            _tmp_2 = _cursor.getString(_cursorIndexOfKeyPoints);
            _tmpKeyPoints = __converters.toStringList(_tmp_2);
            final boolean _tmpIsProcessing;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsProcessing);
            _tmpIsProcessing = _tmp_3 != 0;
            _item = new VoiceNote(_tmpId,_tmpTitle,_tmpFilePath,_tmpDuration,_tmpFileSize,_tmpCreatedAt,_tmpTranscript,_tmpSummary,_tmpKeyPoints,_tmpIsProcessing);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getVoiceNoteById(final long id, final Continuation<? super VoiceNote> $completion) {
    final String _sql = "SELECT * FROM voice_notes WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<VoiceNote>() {
      @Override
      @Nullable
      public VoiceNote call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfFilePath = CursorUtil.getColumnIndexOrThrow(_cursor, "filePath");
          final int _cursorIndexOfDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "duration");
          final int _cursorIndexOfFileSize = CursorUtil.getColumnIndexOrThrow(_cursor, "fileSize");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfTranscript = CursorUtil.getColumnIndexOrThrow(_cursor, "transcript");
          final int _cursorIndexOfSummary = CursorUtil.getColumnIndexOrThrow(_cursor, "summary");
          final int _cursorIndexOfKeyPoints = CursorUtil.getColumnIndexOrThrow(_cursor, "keyPoints");
          final int _cursorIndexOfIsProcessing = CursorUtil.getColumnIndexOrThrow(_cursor, "isProcessing");
          final VoiceNote _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpFilePath;
            _tmpFilePath = _cursor.getString(_cursorIndexOfFilePath);
            final long _tmpDuration;
            _tmpDuration = _cursor.getLong(_cursorIndexOfDuration);
            final long _tmpFileSize;
            _tmpFileSize = _cursor.getLong(_cursorIndexOfFileSize);
            final Date _tmpCreatedAt;
            final Long _tmp;
            if (_cursor.isNull(_cursorIndexOfCreatedAt)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getLong(_cursorIndexOfCreatedAt);
            }
            final Date _tmp_1 = __converters.fromTimestamp(_tmp);
            if (_tmp_1 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.util.Date', but it was NULL.");
            } else {
              _tmpCreatedAt = _tmp_1;
            }
            final String _tmpTranscript;
            if (_cursor.isNull(_cursorIndexOfTranscript)) {
              _tmpTranscript = null;
            } else {
              _tmpTranscript = _cursor.getString(_cursorIndexOfTranscript);
            }
            final String _tmpSummary;
            if (_cursor.isNull(_cursorIndexOfSummary)) {
              _tmpSummary = null;
            } else {
              _tmpSummary = _cursor.getString(_cursorIndexOfSummary);
            }
            final List<String> _tmpKeyPoints;
            final String _tmp_2;
            _tmp_2 = _cursor.getString(_cursorIndexOfKeyPoints);
            _tmpKeyPoints = __converters.toStringList(_tmp_2);
            final boolean _tmpIsProcessing;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsProcessing);
            _tmpIsProcessing = _tmp_3 != 0;
            _result = new VoiceNote(_tmpId,_tmpTitle,_tmpFilePath,_tmpDuration,_tmpFileSize,_tmpCreatedAt,_tmpTranscript,_tmpSummary,_tmpKeyPoints,_tmpIsProcessing);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getVoiceNotesCount(final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM voice_notes";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
