package dev.prepstudio.catalog.api;

import dev.prepstudio.api.catalog.v1.CatalogServiceGrpc;
import dev.prepstudio.api.catalog.v1.GetKeyRequest;
import dev.prepstudio.api.catalog.v1.Key;
import dev.prepstudio.api.catalog.v1.SearchKeysRequest;
import dev.prepstudio.api.catalog.v1.SearchKeysResponse;
import dev.prepstudio.catalog.application.KeyService;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class CatalogGrpcService extends CatalogServiceGrpc.CatalogServiceImplBase {

    private final KeyService keys;

    public CatalogGrpcService(KeyService keys) {
        this.keys = keys;
    }

    @Override
    public void searchKeys(SearchKeysRequest request, StreamObserver<SearchKeysResponse> out) {
        var response = SearchKeysResponse.newBuilder();
        keys.search(request.getQuery(), request.getPlatformId(), request.getLimit())
                .forEach(k -> response.addKeys(CatalogProtoMapper.key(k)));
        out.onNext(response.build());
        out.onCompleted();
    }

    @Override
    public void getKey(GetKeyRequest request, StreamObserver<Key> out) {
        out.onNext(CatalogProtoMapper.key(keys.get(request.getKeyId())));
        out.onCompleted();
    }
}
