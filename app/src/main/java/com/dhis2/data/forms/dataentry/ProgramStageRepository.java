package com.dhis2.data.forms.dataentry;

import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.dhis2.data.forms.dataentry.fields.FieldViewModel;
import com.dhis2.data.forms.dataentry.fields.FieldViewModelFactory;
import com.squareup.sqlbrite2.BriteDatabase;

import org.hisp.dhis.android.core.common.ValueType;
import org.hisp.dhis.android.core.organisationunit.OrganisationUnitModel;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityDataValueModel;

import java.util.List;
import java.util.Locale;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;

import static android.text.TextUtils.isEmpty;

@SuppressWarnings({
        "PMD.AvoidDuplicateLiterals"
})
final class ProgramStageRepository implements DataEntryRepository {
    private static final String QUERY = "SELECT\n" +
            "  Field.id,\n" +
            "  Field.label,\n" +
            "  Field.type,\n" +
            "  Field.mandatory,\n" +
            "  Field.optionSet,\n" +
            "  Value.value,\n" +
            "  Option.name,\n" +
            "  Field.section,\n" +
            "  Field.allowFutureDate\n" +
            "FROM Event\n" +
            "  LEFT OUTER JOIN (\n" +
            "      SELECT\n" +
            "        DataElement.displayName AS label,\n" +
            "        DataElement.valueType AS type,\n" +
            "        DataElement.uid AS id,\n" +
            "        DataElement.optionSet AS optionSet,\n" +
            "        ProgramStageDataElement.sortOrder AS formOrder,\n" +
            "        ProgramStageDataElement.programStage AS stage,\n" +
            "        ProgramStageDataElement.compulsory AS mandatory,\n" +
            "        ProgramStageDataElement.programStageSection AS section,\n" +
            "        ProgramStageDataElement.allowFutureDate AS allowFutureDate\n" +
            "      FROM ProgramStageDataElement\n" +
            "        INNER JOIN DataElement ON DataElement.uid = ProgramStageDataElement.dataElement\n" +
            "    ) AS Field ON (Field.stage = Event.programStage)\n" +
            "  LEFT OUTER JOIN TrackedEntityDataValue AS Value ON (\n" +
            "    Value.event = Event.uid AND Value.dataElement = Field.id\n" +
            "  )\n" +
            "  LEFT OUTER JOIN Option ON (\n" +
            "    Field.optionSet = Option.optionSet AND Value.value = Option.code\n" +
            "  )\n" +
            " %s  " +
            "ORDER BY Field.formOrder ASC;";

    @NonNull
    private final BriteDatabase briteDatabase;

    @NonNull
    private final FieldViewModelFactory fieldFactory;

    @NonNull
    private final String eventUid;

    @Nullable
    private final String sectionUid;

    ProgramStageRepository(@NonNull BriteDatabase briteDatabase,
                           @NonNull FieldViewModelFactory fieldFactory,
                           @NonNull String eventUid, @Nullable String sectionUid) {
        this.briteDatabase = briteDatabase;
        this.fieldFactory = fieldFactory;
        this.eventUid = eventUid;
        this.sectionUid = sectionUid;
    }

    @NonNull
    @Override
    public Flowable<List<FieldViewModel>> list() {
        return briteDatabase
                .createQuery(TrackedEntityDataValueModel.TABLE, prepareStatement())
                .mapToList(this::transform).toFlowable(BackpressureStrategy.LATEST);
    }

    @Override
    public Observable<List<OrganisationUnitModel>> getOrgUnits() {
        return briteDatabase.createQuery(OrganisationUnitModel.TABLE, "SELECT * FROM " + OrganisationUnitModel.TABLE)
                .mapToList(OrganisationUnitModel::create);
    }

    @NonNull
    private FieldViewModel transform(@NonNull Cursor cursor) {
        String dataValue = cursor.getString(5);
        String optionCodeName = cursor.getString(6);

        if (!isEmpty(optionCodeName)) {
            dataValue = optionCodeName;
        }

        return fieldFactory.create(cursor.getString(0), cursor.getString(1),
                ValueType.valueOf(cursor.getString(2)), cursor.getInt(3) == 1,
                cursor.getString(4), dataValue, cursor.getString(7), cursor.getInt(8) == 1);
    }

    @NonNull
    @SuppressFBWarnings("VA_FORMAT_STRING_USES_NEWLINE")
    private String prepareStatement() {
        String where;
        if (isEmpty(sectionUid)) {
            where = String.format(Locale.US, "WHERE Event.uid = '%s'", eventUid);
        } else {
            where = String.format(Locale.US, "WHERE Event.uid = '%s' AND " +
                    "Field.section = '%s'", eventUid, sectionUid);
        }

        return String.format(Locale.US, QUERY, where);
    }
}
