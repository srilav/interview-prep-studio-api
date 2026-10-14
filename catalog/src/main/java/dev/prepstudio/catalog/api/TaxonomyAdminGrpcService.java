package dev.prepstudio.catalog.api;

import dev.prepstudio.api.catalog.v1.Category;
import dev.prepstudio.api.catalog.v1.CreateCategoryRequest;
import dev.prepstudio.api.catalog.v1.CreatePlatformRequest;
import dev.prepstudio.api.catalog.v1.CreateSubcategoryRequest;
import dev.prepstudio.api.catalog.v1.DeleteKeyRequest;
import dev.prepstudio.api.catalog.v1.DeleteKeyResponse;
import dev.prepstudio.api.catalog.v1.GenerateKeyDraftRequest;
import dev.prepstudio.api.catalog.v1.Key;
import dev.prepstudio.api.catalog.v1.KeyDraft;
import dev.prepstudio.api.catalog.v1.ListKeysRequest;
import dev.prepstudio.api.catalog.v1.ListKeysResponse;
import dev.prepstudio.api.catalog.v1.ListTreeRequest;
import dev.prepstudio.api.catalog.v1.MoveKeyRequest;
import dev.prepstudio.api.catalog.v1.Platform;
import dev.prepstudio.api.catalog.v1.Subcategory;
import dev.prepstudio.api.catalog.v1.TaxonomyAdminServiceGrpc;
import dev.prepstudio.api.catalog.v1.Tree;
import dev.prepstudio.api.catalog.v1.UpsertKeyRequest;
import dev.prepstudio.catalog.application.KeyDraftService;
import dev.prepstudio.catalog.application.KeyService;
import dev.prepstudio.catalog.application.TaxonomyService;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.Map;

import static dev.prepstudio.catalog.api.CatalogProtoMapper.content;
import static dev.prepstudio.catalog.api.CatalogProtoMapper.difficulty;
import static dev.prepstudio.catalog.api.CatalogProtoMapper.kind;
import static dev.prepstudio.catalog.api.CatalogProtoMapper.status;

@GrpcService
public class TaxonomyAdminGrpcService extends TaxonomyAdminServiceGrpc.TaxonomyAdminServiceImplBase {

    private final TaxonomyService taxonomy;
    private final KeyService keys;
    private final KeyDraftService drafts;

    public TaxonomyAdminGrpcService(TaxonomyService taxonomy, KeyService keys, KeyDraftService drafts) {
        this.taxonomy = taxonomy;
        this.keys = keys;
        this.drafts = drafts;
    }

    @Override
    public void listTree(ListTreeRequest request, StreamObserver<Tree> out) {
        reply(out, CatalogProtoMapper.tree(taxonomy.tree()));
    }

    @Override
    public void createPlatform(CreatePlatformRequest request, StreamObserver<Platform> out) {
        var platform = taxonomy.createPlatform(request.getName(), kind(request.getKind()));
        reply(out, CatalogProtoMapper.platform(platform, Map.of()));
    }

    @Override
    public void createCategory(CreateCategoryRequest request, StreamObserver<Category> out) {
        var category = taxonomy.createCategory(request.getPlatformId(), request.getName());
        reply(out, CatalogProtoMapper.category(category, Map.of()));
    }

    @Override
    public void createSubcategory(CreateSubcategoryRequest request, StreamObserver<Subcategory> out) {
        var sub = taxonomy.createSubcategory(request.getPlatformId(), request.getCategoryId(), request.getName());
        reply(out, CatalogProtoMapper.subcategory(sub, 0));
    }

    @Override
    public void listKeys(ListKeysRequest request, StreamObserver<ListKeysResponse> out) {
        var response = ListKeysResponse.newBuilder();
        keys.list(request.getSubcategoryId(), status(request.getStatus()))
                .forEach(k -> response.addKeys(CatalogProtoMapper.key(k)));
        reply(out, response.build());
    }

    @Override
    public void upsertKey(UpsertKeyRequest request, StreamObserver<Key> out) {
        Key k = request.getKey();
        var saved = keys.upsert(k.getId(), k.getSubcategoryId(), content(k), status(k.getStatus()));
        reply(out, CatalogProtoMapper.key(saved));
    }

    @Override
    public void moveKey(MoveKeyRequest request, StreamObserver<Key> out) {
        reply(out, CatalogProtoMapper.key(keys.move(request.getKeyId(), request.getTargetSubcategoryId())));
    }

    @Override
    public void deleteKey(DeleteKeyRequest request, StreamObserver<DeleteKeyResponse> out) {
        keys.delete(request.getKeyId());
        reply(out, DeleteKeyResponse.getDefaultInstance());
    }

    @Override
    public void generateKeyDraft(GenerateKeyDraftRequest request, StreamObserver<KeyDraft> out) {
        var draft = drafts.draft(request.getSubcategoryId(), request.getTerm(), difficulty(request.getDifficulty()));
        reply(out, KeyDraft.newBuilder()
                .setShortDefinition(draft.shortDefinition())
                .setDetail(draft.detail())
                .setJavaExample(draft.javaExample())
                .build());
    }

    private static <T> void reply(StreamObserver<T> out, T value) {
        out.onNext(value);
        out.onCompleted();
    }
}
