package dev.by1337.bc.bd;

import blib.com.mojang.serialization.Codec;
import blib.com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.by1337.bc.BCasesApi;
import org.by1337.blib.chat.placeholder.Placeholder;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;

public class CaseKey extends Placeholder {
    public static final Codec<CaseKey> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(CaseKey::id),
            Codec.LONG.fieldOf("issue_date").forGetter(CaseKey::issueDate),
            Codec.LONG.fieldOf("removal_date").forGetter(CaseKey::removalDate)
    ).apply(instance, CaseKey::new));

    public static final Codec<List<CaseKey>> LIST_CODEC = CODEC.listOf();
    private final String id;
    private final long issueDate;
    private final long removalDate;

    public CaseKey(String id, long issueDate, long removalDate) {
        this.id = id;
        this.issueDate = issueDate;
        this.removalDate = removalDate;
    }

    @ApiStatus.Internal
    @ApiStatus.Experimental
    public void initPlaceholders(BCasesApi bCasesApi) {
        if (placeholders.containsKey("{issue_date}")) return;
        registerPlaceholder("{issue_date}", () -> bCasesApi.getTimeFormatter().getFormat(this.issueDate));
        registerPlaceholder("{removal_date}", () -> bCasesApi.getTimeFormatter().getFormat(this.removalDate));
    }

    public String id() {
        return id;
    }

    public long issueDate() {
        return issueDate;
    }

    public long removalDate() {
        return removalDate;
    }
}
