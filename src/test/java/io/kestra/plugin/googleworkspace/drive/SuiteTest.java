package io.kestra.plugin.googleworkspace.drive;

import com.google.common.io.CharStreams;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.storages.StorageInterface;
import io.kestra.core.utils.IdUtils;
import io.kestra.core.utils.TestsUtils;
import io.kestra.plugin.googleworkspace.UtilsTest;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import jakarta.inject.Inject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@MicronautTest
class SuiteTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Inject
    private StorageInterface storageInterface;

    @Test
    void run() throws Exception {
        URI source = storageInterface.put(
            null,
            new URI("/" + IdUtils.create()),
            new FileInputStream(new File(Objects.requireNonNull(SuiteTest.class.getClassLoader()
                    .getResource("examples/addresses.csv"))
                .toURI()))
        );

        URI source2 = storageInterface.put(
            null,
            new URI("/" + IdUtils.create()),
            new FileInputStream(new File(Objects.requireNonNull(SuiteTest.class.getClassLoader()
                    .getResource("examples/addresses2.csv"))
                .toURI()))
        );

        Create create = Create.builder()
            .id(SuiteTest.class.getSimpleName())
            .type(Create.class.getName())
            .name(IdUtils.create())
            .parents(List.of("0AMcI1s7ZFzh0Uk9PVA"))
            .mimeType("application/vnd.google-apps.folder")
            .serviceAccount(UtilsTest.serviceAccount())
            .build();

        Create.Output createRun = create.run(TestsUtils.mockRunContext(runContextFactory, create, Map.of()));

        assertThat(createRun.getFile().getParents(), contains("0AMcI1s7ZFzh0Uk9PVA"));

        Upload upload = Upload.builder()
            .id(SuiteTest.class.getSimpleName())
            .type(Upload.class.getName())
            .from(source.toString())
            .parents(List.of(createRun.getFile().getId()))
            .name(IdUtils.create())
            .contentType("text/csv")
            .mimeType("application/vnd.google-apps.spreadsheet")
            .serviceAccount(UtilsTest.serviceAccount())
            .build();

        Upload.Output uploadRun = upload.run(TestsUtils.mockRunContext(runContextFactory, upload, Map.of()));

        assertThat(uploadRun.getFile().getSize(), greaterThan(0L));

        Export export = Export.builder()
            .id(SuiteTest.class.getSimpleName())
            .type(Export.class.getName())
            .fileId(uploadRun.getFile().getId())
            .contentType("text/csv")
            .serviceAccount(UtilsTest.serviceAccount())
            .build();

        Export.Output exportRun = export.run(TestsUtils.mockRunContext(runContextFactory, export, Map.of()));

        assertThat(exportRun.getFile().getName(), is(uploadRun.getFile().getName()));
        assertThat(exportRun.getFile().getSize(), greaterThan(0L));
        assertThat(exportRun.getFile().getVersion(), notNullValue());
        assertThat(exportRun.getFile().getMimeType(), notNullValue());
        assertThat(exportRun.getFile().getCreatedTime(), is(uploadRun.getFile().getCreatedTime()));
        assertThat(exportRun.getFile().getParents().equals(uploadRun.getFile().getParents()), is(true));
        assertThat(exportRun.getFile().getTrashed(), is(uploadRun.getFile().getTrashed()));

        InputStream get = storageInterface.get(null, exportRun.getUri());
        String getContent = CharStreams.toString(new InputStreamReader(get));

        assertThat(getContent, containsString("John,Doe"));
        assertThat(getContent, containsString("Desert City,CO,123"));

        Upload upload2 = Upload.builder()
            .id(SuiteTest.class.getSimpleName())
            .type(Upload.class.getName())
            .from(source2.toString())
            .fileId(uploadRun.getFile().getId())
            .name(IdUtils.create())
            .contentType("text/csv")
            .mimeType("application/vnd.google-apps.spreadsheet")
            .serviceAccount(UtilsTest.serviceAccount())
            .build();

        Upload.Output upload2Run = upload2.run(TestsUtils.mockRunContext(runContextFactory, upload, Map.of()));

        assertThat(upload2Run.getFile().getSize(), is(1024L));

        Export export2 = Export.builder()
            .id(SuiteTest.class.getSimpleName())
            .type(Export.class.getName())
            .fileId(uploadRun.getFile().getId())
            .contentType("text/csv")
            .serviceAccount(UtilsTest.serviceAccount())
            .build();

        Export.Output exportRun2 = export2.run(TestsUtils.mockRunContext(runContextFactory, export, Map.of()));

        assertThat(exportRun2.getFile().getName(), is(upload2Run.getFile().getName()));
        assertThat(exportRun2.getFile().getSize(), is(upload2Run.getFile().getSize()));
        assertThat(exportRun2.getFile().getVersion(), is(upload2Run.getFile().getVersion()));
        assertThat(exportRun2.getFile().getMimeType(), notNullValue());
        assertThat(exportRun2.getFile().getCreatedTime(), is(upload2Run.getFile().getCreatedTime()));
        assertThat(exportRun2.getFile().getParents().equals(upload2Run.getFile().getParents()), is(true));
        assertThat(exportRun2.getFile().getTrashed(), is(upload2Run.getFile().getTrashed()));

        InputStream get2 = storageInterface.get(null, exportRun2.getUri());
        String getContent2 = CharStreams.toString(new InputStreamReader(get2));

        assertThat(getContent2, containsString("Jane,Doe"));
        assertThat(getContent2, containsString("Desert City,CO,123"));

        Delete delete = Delete.builder()
            .id(SuiteTest.class.getSimpleName())
            .type(Delete.class.getName())
            .fileId(uploadRun.getFile().getId())
            .serviceAccount(UtilsTest.serviceAccount())
            .build();

        Delete.Output deleteRun = delete.run(TestsUtils.mockRunContext(runContextFactory, delete, Map.of()));

        assertThat(deleteRun.getFileId(), is(exportRun.getFile().getId()));

        Delete deleteFolder = Delete.builder()
            .id(SuiteTest.class.getSimpleName())
            .type(Delete.class.getName())
            .fileId(createRun.getFile().getId())
            .serviceAccount(UtilsTest.serviceAccount())
            .build();

        Delete.Output deleteFolderRun = deleteFolder.run(TestsUtils.mockRunContext(runContextFactory, deleteFolder, Map.of()));

        assertThat(deleteFolderRun.getFileId(), is(createRun.getFile().getId()));

        assertThrows(Exception.class, () -> {
            export.run(TestsUtils.mockRunContext(runContextFactory, upload, Map.of()));
        });
    }

    @Test
    void binary() throws Exception {
        File file = new File(Objects.requireNonNull(SuiteTest.class.getClassLoader()
                .getResource("examples/addresses.zip"))
            .toURI());

        URI source = storageInterface.put(
            null,
            new URI("/" + IdUtils.create()),
            new FileInputStream(file)
        );

        Upload upload = Upload.builder()
            .id(SuiteTest.class.getSimpleName())
            .type(Upload.class.getName())
            .from(source.toString())
            .parents(List.of("0AMcI1s7ZFzh0Uk9PVA"))
            .name(IdUtils.create())
            .contentType("application/zip")
            .serviceAccount(UtilsTest.serviceAccount())
            .build();

        Upload.Output uploadRun = upload.run(TestsUtils.mockRunContext(runContextFactory, upload, Map.of()));

        assertThat(uploadRun.getFile().getSize(), is(390L));

        Download download = Download.builder()
            .id(SuiteTest.class.getSimpleName())
            .type(Export.class.getName())
            .fileId(uploadRun.getFile().getId())
            .serviceAccount(UtilsTest.serviceAccount())
            .build();

        Download.Output downloadRun = download.run(TestsUtils.mockRunContext(runContextFactory, upload, Map.of()));

        assertThat(downloadRun.getFile().getName(), is(uploadRun.getFile().getName()));
        assertThat(downloadRun.getFile().getSize(), is(uploadRun.getFile().getSize()));
        assertThat(downloadRun.getFile().getVersion(), is(uploadRun.getFile().getVersion()));
        assertThat(downloadRun.getFile().getMimeType(), notNullValue());
        assertThat(downloadRun.getFile().getCreatedTime(), is(uploadRun.getFile().getCreatedTime()));
        assertThat(downloadRun.getFile().getParents().equals(uploadRun.getFile().getParents()), is(true));
        assertThat(downloadRun.getFile().getTrashed(), is(uploadRun.getFile().getTrashed()));

        InputStream get = storageInterface.get(null, downloadRun.getUri());

        assertThat(
            CharStreams.toString(new InputStreamReader(get)),
            is(CharStreams.toString(new InputStreamReader(new FileInputStream(file))))
        );

        Delete delete = Delete.builder()
            .id(SuiteTest.class.getSimpleName())
            .type(Delete.class.getName())
            .fileId(uploadRun.getFile().getId())
            .serviceAccount(UtilsTest.serviceAccount())
            .build();

        Delete.Output deleteRun = delete.run(TestsUtils.mockRunContext(runContextFactory, upload, Map.of()));

        assertThat(deleteRun.getFileId(), is(downloadRun.getFile().getId()));
    }
}
