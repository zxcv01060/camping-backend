package tw.edu.ntub.imd.camping.controller;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import tw.edu.ntub.birc.common.util.MathUtils;
import tw.edu.ntub.birc.common.wrapper.date.DateTimePattern;
import tw.edu.ntub.imd.camping.bean.*;
import tw.edu.ntub.imd.camping.service.CityService;
import tw.edu.ntub.imd.camping.service.ProductGroupService;
import tw.edu.ntub.imd.camping.util.http.BindingResultUtils;
import tw.edu.ntub.imd.camping.util.http.ResponseEntityBuilder;
import tw.edu.ntub.imd.camping.util.json.object.CollectionObjectData;
import tw.edu.ntub.imd.camping.util.json.object.ObjectData;
import tw.edu.ntub.imd.camping.util.json.object.SingleValueObjectData;
import tw.edu.ntub.imd.camping.validation.CreateProductGroup;
import tw.edu.ntub.imd.camping.validation.UpdateProduct;
import tw.edu.ntub.imd.camping.validation.UpdateProductGroup;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Tag(name = "Product", description = "商品相關API")
@RestController
@RequestMapping(path = "/product-group")
public class ProductGroupController {
    private final ProductGroupService productGroupService;
    private final CityService cityService;

    @Operation(
            tags = "Product",
            method = "POST",
            summary = "上架商品群組",
            description = "上架商品群組，因有檔案上傳，需使用Form-Data呼叫API",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(implementation = ProductGroupBean.class)
                    )
            ),
            responses = @ApiResponse(
                    responseCode = "200",
                    description = "上架成功",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE
                    )
            )
    )
    @PostMapping(path = "")
    public ResponseEntity<String> create(
            @RequestBody @Validated(CreateProductGroup.class) ProductGroupBean productGroup,
            BindingResult bindingResult
    ) {
        BindingResultUtils.validate(bindingResult);
        productGroupService.save(productGroup);
        return ResponseEntityBuilder.buildSuccessMessage("上架成功");
    }

    @Operation(
            tags = "Product",
            method = "GET",
            summary = "查詢商品類型",
            description = "查詢商品類型",
            responses = @ApiResponse(
                    responseCode = "200",
                    description = "查詢成功",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = ProductTypeBean.class))
                    )
            )
    )
    @GetMapping(path = "/product/type")
    public ResponseEntity<String> searchProductType() {
        return ResponseEntityBuilder.success("查詢成功")
                .data(productGroupService.searchAllProductType(), this::addProductTypeToObjectData)
                .build();
    }

    private void addProductTypeToObjectData(ObjectData data, ProductTypeBean type) {
        data.add("id", type.getId());
        data.add("name", type.getName());
    }

    @Operation(
            tags = "Product",
            method = "GET",
            summary = "查詢商品群組列表過濾器資料",
            description = "查詢過濾器所需的地點、商品種類",
            responses = @ApiResponse(
                    responseCode = "200",
                    description = "查詢成功",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProductGroupListFilterSchema.class)
                    )
            )
    )
    @GetMapping(path = "/filter")
    public ResponseEntity<String> searchProductGroupFilter() {
        List<ProductTypeBean> typeList = productGroupService.searchAllProductType();
        ObjectData data = new ObjectData();
        CollectionObjectData collectionData = data.createCollectionData();
        collectionData.add("type", typeList, this::addProductTypeToObjectData);
        addCityToObjectData(cityService.searchAllEnableCity(), data);
        return ResponseEntityBuilder.success("查詢成功")
                .data(data)
                .build();
    }

    private void addCityToObjectData(List<CityBean> cityList, ObjectData data) {
        CollectionObjectData collectionObjectData = data.createCollectionData();
        collectionObjectData.add("cityArray", cityList, (cityData, cityBean) -> {
            cityData.add("id", cityBean.getId());
            cityData.add("name", cityBean.getName());
            cityData.add("areaName", cityBean.getAreaName());
        });
    }

    @Operation(
            tags = "Product",
            method = "GET",
            summary = "查詢商品群組列表",
            description = "查詢商品群組列表",
            parameters = {
                    @Parameter(name = "borrowStartDate", description = "可租借起始時間", example = "2020/08/14"),
                    @Parameter(name = "borrowEndDate", description = "可租借結束時間", example = "2020/08/20"),
                    @Parameter(name = "cityAreaName", description = "城市區域名稱", example = "中正區"),
                    @Parameter(
                            name = "typeArray",
                            description = "商品類型陣列",
                            example = "[1, 2, 3]"
                    ),
                    @Parameter(name = "priceRange", description = "價格範圍(0: 0 ~ 2,000/ 1: 2,001 ~ 4,000)", example = "0")
            },
            responses = @ApiResponse(
                    responseCode = "200",
                    description = "查詢成功",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CanBorrowProductGroupBean.class)
                    )
            )
    )
    @GetMapping(path = "")
    public ResponseEntity<String> searchCanBorrowProductGroup(ProductGroupFilterDataBean filterData) {
        return ResponseEntityBuilder.success("查詢成功")
                .data(productGroupService.searchCanBorrowProductGroup(filterData), getCanBorrowProductGroupConsumer())
                .build();
    }

    private BiConsumer<ObjectData, CanBorrowProductGroupBean> getCanBorrowProductGroupConsumer() {
        return (data, canBorrowProductGroup) -> {
            data.add("id", canBorrowProductGroup.getId());
            data.add("name", canBorrowProductGroup.getName());
            data.add("coverImage", canBorrowProductGroup.getCoverImage());
            data.add("price", canBorrowProductGroup.getPrice());
            data.add("borrowStartDate", canBorrowProductGroup.getBorrowStartDate(), DateTimePattern.of("yyyy/MM/dd HH:mm"));
            data.add("borrowEndDate", canBorrowProductGroup.getBorrowEndDate(), DateTimePattern.of("yyyy/MM/dd HH:mm"));
            data.add("city", canBorrowProductGroup.getCity());
            data.add("userName", canBorrowProductGroup.getUserName());
            data.addStringArray("productTypeArray", canBorrowProductGroup.getProductTypeArray());
            data.add("comment", MathUtils.round(canBorrowProductGroup.getComment(), 2));
        };
    }

    @Operation(
            tags = "Product",
            method = "GET",
            summary = "查詢使用者商品群組列表",
            description = "查詢使用者商品群組列表",
            parameters = {
                    @Parameter(name = "account", description = "使用者帳號", example = "admin")
            },
            responses = @ApiResponse(
                    responseCode = "200",
                    description = "查詢成功",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CanBorrowProductGroupBean.class)
                    )
            )
    )
    @GetMapping(path = "/user/{account}")
    public ResponseEntity<String> searchCanBorrowProductGroup(@PathVariable String account) {
        return ResponseEntityBuilder.success("查詢成功")
                .data(productGroupService.searchCanBorrowProductGroupByCreateAccount(account), getCanBorrowProductGroupConsumer())
                .build();
    }

    @Operation(
            tags = "Product",
            method = "GET",
            summary = "查詢商品群組",
            description = "查詢商品群組",
            parameters = @Parameter(name = "id", description = "商品群組編號", example = "1"),
            responses = @ApiResponse(
                    responseCode = "200",
                    description = "查詢成功",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProductGroupContentSchema.class)
                    )
            )
    )
    @GetMapping(path = "/{id}")
    public ResponseEntity<String> getGroupContent(
            @PathVariable(name = "id")
            @Positive(message = "群組編號不能為負數")
                    int id
    ) {
        Optional<ProductGroupBean> optionalGroup = productGroupService.getById(id);
        if (optionalGroup.isPresent()) {
            ProductGroupBean productGroupBean = optionalGroup.get();
            ObjectData data = getGroupContentData(productGroupBean);
            return ResponseEntityBuilder.success()
                    .message("查詢成功")
                    .data(data)
                    .build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    private ObjectData getGroupContentData(ProductGroupBean productGroupBean) {
        ObjectData data = new ObjectData();
        data.add("name", productGroupBean.getName());
        data.add("coverImage", productGroupBean.getCoverImage());
        CityBean city = productGroupBean.getCity();
        ObjectData cityData = data.addObject("city");
        cityData.add("id", city.getId());
        cityData.add("name", city.getName());
        cityData.add("areaName", city.getAreaName());
        data.add("price", productGroupBean.getPrice());
        data.add("borrowStartDate", productGroupBean.getBorrowStartDate().format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")));
        data.add("borrowEndDate", productGroupBean.getBorrowEndDate().format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")));
        UserBean createUser = productGroupBean.getCreateUser();
        data.add("productOwnerAccount", createUser.getAccount());
        data.add("productOwnerName", createUser.getNickName());
        data.add("contact", createUser.getEmail());
        data.add("comment", productGroupBean.getComment());

        CollectionObjectData collectionData = data.createCollectionData();
        collectionData.add("productArray", productGroupBean.getProductArray(), this::addProductData);
        return data;
    }

    private void addProductData(ObjectData productData, ProductBean product) {
        productData.add("id", product.getId());
        productData.add("type", product.getTypeName());
        productData.add("name", product.getName());
        productData.add("count", product.getCount());
        productData.add("brand", product.getBrand());
        productData.add("appearance", product.getAppearance());
        productData.add("useInformation", product.getUseInformation());
        productData.add("brokenCompensation", product.getBrokenCompensation());
        productData.add("relatedLink", product.getRelatedLink());
        productData.add("memo", product.getMemo());
        CollectionObjectData productCollectionData = productData.createCollectionData();
        if (CollectionUtils.isNotEmpty(product.getImageArray())) {
            productCollectionData.add("imageArray", product.getImageArray(), this::addProductImageData);
        } else {
            productData.addStringArray("imageArray", new String[0]);
        }
    }

    private void addProductImageData(ObjectData productImageData, ProductImageBean productImage) {
        productImageData.add("id", productImage.getId());
        productImageData.add("url", productImage.getUrl());
    }

    @Operation(
            tags = "Product",
            method = "GET",
            summary = "查詢商品群組(編輯)",
            description = "查詢商品群組(編輯)",
            parameters = @Parameter(name = "id", description = "商品群組編號", example = "1"),
            responses = @ApiResponse(
                    responseCode = "200",
                    description = "查詢成功",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UpdateProductGroupContentSchema.class)
                    )
            )
    )
    @GetMapping(path = "/update/{id}")
    public ResponseEntity<String> getUpdateGroupContent(
            @PathVariable(name = "id")
            @Positive(message = "群組編號不能為負數")
                    int id
    ) {
        Optional<ProductGroupBean> optionalGroup = productGroupService.getById(id);
        if (optionalGroup.isPresent()) {
            ProductGroupBean productGroupBean = optionalGroup.get();
            ObjectData data = getGroupContentData(productGroupBean);
            data.add("price", productGroupBean.getPrice());
            data.add("bankAccount", productGroupBean.getCreateUser().getBankAccount());
            return ResponseEntityBuilder.success()
                    .message("查詢成功")
                    .data(data)
                    .build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
            tags = "Product",
            method = "PATCH",
            summary = "更新商品群組",
            description = "更新商品群組以及商品",
            parameters = @Parameter(name = "id", description = "商品群組編號", example = "1"),
            responses = @ApiResponse(
                    responseCode = "200",
                    description = "更新成功",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE
                    )
            )
    )
    @PatchMapping(path = "/{id}")
    public ResponseEntity<String> update(
            @PathVariable(name = "id") @Positive(message = "群組編號 - 應為大於0的數字") Integer id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(implementation = UpdateProductGroupSchema.class)
                    )
            ) @RequestBody @Validated(UpdateProductGroup.class) ProductGroupBean productGroup,
            BindingResult bindingResult
    ) {
        BindingResultUtils.validate(bindingResult);
        productGroupService.update(id, productGroup);
        return ResponseEntityBuilder.buildSuccessMessage("更新成功");
    }

    @Operation(
            tags = "Product",
            method = "PATCH",
            summary = "更新商品",
            description = "能一次更新多筆商品，需在傳入的商品物件中加入商品編號",
            parameters = {
                    @Parameter(name = "groupId", description = "商品群組編號", example = "1")
            },
            responses = @ApiResponse(
                    responseCode = "200",
                    description = "更新成功",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE
                    )
            )
    )
    @PatchMapping(path = "/{groupId}/product")
    public ResponseEntity<String> updateProduct(
            @PathVariable(name = "groupId") @Positive(message = "商品群組編號 - 應為大於0的數字") Integer groupId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UpdateProductSchema.class)
                    )
            ) @RequestBody @Validated(UpdateProduct.class) List<@Valid ProductBean> productList,
            BindingResult bindingResult
    ) {
        BindingResultUtils.validate(bindingResult);
        productGroupService.updateProduct(
                productList.parallelStream()
                        .peek(productBean -> productBean.setGroupId(groupId))
                        .collect(Collectors.toList())
        );
        return ResponseEntityBuilder.buildSuccessMessage("更新成功");
    }

    @Operation(
            tags = "Product",
            method = "PATCH",
            summary = "更新商品",
            description = "更新商品",
            parameters = {
                    @Parameter(name = "groupId", description = "商品群組編號", example = "1"),
                    @Parameter(name = "productId", description = "商品編號", example = "1")
            },
            responses = @ApiResponse(
                    responseCode = "200",
                    description = "更新成功",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE
                    )
            )
    )
    @PatchMapping(path = "/{groupId}/product/{productId}")
    public ResponseEntity<String> updateProduct(
            @PathVariable(name = "productId") @Positive(message = "商品群組編號 - 應為大於0的數字") Integer productId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UpdateProductSchema.class)
                    )
            ) @Validated(UpdateProduct.class) @RequestBody ProductBean product,
            BindingResult bindingResult
    ) {
        BindingResultUtils.validate(bindingResult);
        product.setId(productId);
        productGroupService.updateProduct(Collections.singletonList(product));
        return ResponseEntityBuilder.buildSuccessMessage("更新成功");
    }

    @Operation(
            tags = "Product",
            method = "DELETE",
            summary = "刪除商品群組",
            description = "刪除商品群組",
            parameters = @Parameter(name = "id", description = "商品群組編號", example = "1"),
            responses = @ApiResponse(
                    responseCode = "200",
                    description = "刪除成功",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE
                    )
            )
    )
    @DeleteMapping(path = "/{id}")
    public ResponseEntity<String> delete(@PathVariable(name = "id") @Positive(message = "編號 - 應為大於0的數字") Integer id) {
        productGroupService.delete(id);
        return ResponseEntityBuilder.buildSuccessMessage("刪除成功");
    }

    @Operation(
            tags = "Product",
            method = "DELETE",
            summary = "刪除商品",
            description = "刪除商品",
            parameters = @Parameter(name = "productId", description = "商品編號", example = "1"),
            responses = @ApiResponse(
                    responseCode = "200",
                    description = "刪除成功",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE
                    )
            )
    )
    @DeleteMapping(path = "/product/{productId}")
    public ResponseEntity<String> deleteProduct(
            @PathVariable(name = "productId") @Positive(message = "編號 - 應為大於0的數字") Integer productId
    ) {
        productGroupService.deleteProduct(productId);
        return ResponseEntityBuilder.buildSuccessMessage("刪除成功");
    }

    @Operation(
            tags = "Product",
            method = "DELETE",
            summary = "刪除商品圖片",
            description = "刪除商品圖片",
            parameters = @Parameter(name = "imageId", description = "商品圖片編號", example = "1"),
            responses = @ApiResponse(
                    responseCode = "200",
                    description = "刪除成功",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE
                    )
            )
    )
    @DeleteMapping(path = "/product/image/{imageId}")
    public ResponseEntity<String> deleteProductImage(
            @PathVariable(name = "imageId") @Positive(message = "編號 - 應為大於0的數字") Integer imageId
    ) {
        productGroupService.deleteProductImage(imageId);
        return ResponseEntityBuilder.buildSuccessMessage("刪除成功");
    }

    @Operation(
            tags = "Product",
            method = "POST",
            summary = "新增商品評價",
            description = "評價商品，不得重複評價，評價分數介於1 <= comment <= 5",
            parameters = @Parameter(name = "id", description = "商品群組編號", example = "1"),
            responses = @ApiResponse(
                    responseCode = "200",
                    description = "評價成功",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE
                    )
            )
    )
    @PostMapping(path = "/{id}/comment")
    public ResponseEntity<String> createComment(
            @PathVariable @Positive(message = "編號 - 應為大於0的數字") int id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = ProductGroupCommentSchema.class
                            )
                    )
            ) @RequestBody String requestBodyJsonString
    ) {
        ObjectData requestBody = new ObjectData(requestBodyJsonString);
        Integer comment = requestBody.getInt("comment");
        productGroupService.createComment(id, comment.byteValue());
        return ResponseEntityBuilder.buildSuccessMessage("評價成功");
    }

    @Operation(
            tags = "Product",
            method = "GET",
            summary = "查詢商品子類型",
            description = "查詢商品子類型",
            responses = @ApiResponse(
                    responseCode = "200",
                    description = "查詢成功",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProductSubTypeSchema.class)
                    )
            )
    )
    @GetMapping(path = "/brand")
    public ResponseEntity<String> searchBrand() {
        return ResponseEntityBuilder.success("查詢成功")
                .data(productGroupService.searchAllBrand(), (data, productBrand) -> {
                    data.add("id", productBrand.getId());
                    data.add("name", productBrand.getName());
                })
                .build();
    }

    @Operation(
            tags = "Product",
            method = "GET",
            summary = "查詢商品類型",
            description = "依據品牌搜尋商品類型",
            parameters = @Parameter(name = "brand", description = "商品品牌編號", required = true, example = "1"),
            responses = @ApiResponse(
                    responseCode = "200",
                    description = "查詢成功",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProductSubTypeSchema.class)
                    )
            )
    )
    @GetMapping(path = "/type", params = "brand")
    public ResponseEntity<String> searchProductByBrand(
            @RequestParam
            @NotNull(message = "品牌 - 未填寫")
            @Positive(message = "品牌 - 應為大於0的數字")
                    Integer brand
    ) {
        return ResponseEntityBuilder.success("查詢成功")
                .data(productGroupService.searchTypeByBrand(brand), (data, productType) -> {
                    data.add("id", productType.getId());
                    data.add("name", productType.getName());
                })
                .build();
    }

    @Operation(
            tags = "Product",
            method = "GET",
            summary = "查詢商品子類型",
            description = "查詢商品子類型",
            parameters = {
                    @Parameter(name = "brand", description = "商品品牌編號", required = true, example = "1"),
                    @Parameter(name = "type", description = "商品類型編號", required = true, example = "1")
            },
            responses = @ApiResponse(
                    responseCode = "200",
                    description = "查詢成功",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProductSubTypeSchema.class)
                    )
            )
    )
    @GetMapping(path = "/sub-type")
    public ResponseEntity<String> searchSubType(
            @RequestParam
            @NotNull(message = "品牌 - 未填寫")
            @Positive(message = "品牌 - 應為大於0的數字")
                    Integer brand,
            @RequestParam
            @NotNull(message = "商品類型 - 未填寫")
            @Positive(message = "商品類型 - 應為大於0的數字")
                    Integer type
    ) {
        return ResponseEntityBuilder.success("查詢成功")
                .data(productGroupService.searchSubTypeByBrandAndType(brand, type), (data, productSubType) -> {
                    data.add("id", productSubType.getId());
                    data.add("name", productSubType.getName());
                })
                .build();
    }

    @Operation(
            tags = "Product",
            method = "GET",
            summary = "查詢商品建議售價",
            description = "查詢商品建議售價",
            parameters = {
                    @Parameter(name = "brand", description = "商品品牌編號", required = true, example = "1"),
                    @Parameter(name = "type", description = "商品類型編號", required = true, example = "1"),
                    @Parameter(name = "subType", description = "商品子類型編號", required = true, example = "1")
            },
            responses = @ApiResponse(
                    responseCode = "200",
                    description = "查詢成功",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RecommendProductPriceSchema.class)
                    )
            )
    )
    @GetMapping(path = "/recommend-price")
    public ResponseEntity<String> getRecommendPrice(
            @RequestParam
            @NotNull(message = "品牌 - 未填寫")
            @Positive(message = "品牌 - 應為大於0的數字")
                    Integer brand,
            @RequestParam
            @NotNull(message = "商品類型 - 未填寫")
            @Positive(message = "商品類型 - 應為大於0的數字")
                    Integer type,
            @RequestParam
            @NotNull(message = "商品子類型 - 未填寫")
            @Positive(message = "商品子類型 - 應為大於0的數字")
                    Integer subType
    ) {
        return ResponseEntityBuilder.success("查詢成功")
                .data(SingleValueObjectData.create("price", productGroupService.getRecommendPrice(brand, type, subType)))
                .build();
    }

    // |---------------------------------------------------------------------------------------------------------------------------------------------|
    // |---------------------------------------------------------以下為Swagger所需使用的Schema---------------------------------------------------------|
    // |---------------------------------------------------------------------------------------------------------------------------------------------|

    @Schema(name = "商品群組列表過濾器資料", description = "商品群組列表過濾器資料")
    @Data
    private static class ProductGroupListFilterSchema {
        @ArraySchema(minItems = 1, maxItems = 10, uniqueItems = true, schema = @Schema(description = "商品類型"))
        private ProductTypeBean[] type;
        @Schema(description = "地點")
        private CityBean city;
    }

    @Schema(name = "商品群組內容", description = "商品群組內容")
    @Data
    private static class ProductGroupContentSchema {
        @Schema(description = "商品群組名稱", example = "便宜帳篷、桌椅三件套，限時特價$3990")
        private String name;
        @Schema(description = "城市", example = "台北市 中正區")
        private String city;
        @Schema(description = "封面圖", example = "https://www.ntub.edu.tw/var/file/0/1000/img/1595/logo.png")
        private String coverImage;
        @Schema(description = "租借價格", minimum = "0", example = "$ 3,990")
        private String price;
        @Schema(description = "可租借起始時間", example = "2020/08/14 15:00")
        private String borrowStartDate;
        @Schema(description = "可租借結束時間", example = "2020/08/20 00:00")
        private String borrowEndDate;
        @Schema(description = "出租者帳號(暱稱)", example = "admin(管理員)")
        private String productOwnerName;
        @Schema(description = "聯絡方式", example = "10646007@ntub.edu.tw")
        private String contact;
        @Schema(description = "評價", example = "2.5")
        private Double comment;
        @ArraySchema(minItems = 0, uniqueItems = true, schema = @Schema(description = "商品陣列"))
        private ProductContentSchema productArray;

        @Hidden
        @Data
        private static class ProductContentSchema {
            @Schema(description = "商品編號", minimum = "1", example = "6")
            private Integer id;
            @Schema(description = "商品類型", example = "睡帳")
            private String type;
            @Schema(description = "商品名稱", example = "OO牌睡帳")
            private String name;
            @Schema(description = "商品數量", example = "2")
            private Integer count;
            @Schema(description = "商品品牌", example = "OO")
            private String brand;
            @Schema(description = "商品外觀狀況", example = "300cm*300cm*250cm(高)")
            private String appearance;
            @Schema(description = "使用說明", example = "內附搭帳棚說明書")
            private String useInformation;
            @Schema(description = "損壞賠償", example = "缺少零件：1/$200、布劃破：$1000")
            private String brokenCompensation;
            @Schema(description = "備註", nullable = true, example = "附有教學影片，若在搭設過程有疑問，都可以聯絡我")
            private String memo;
            @ArraySchema(minItems = 0, schema = @Schema(description = "商品圖片陣列", implementation = ProductImageContentSchema.class))
            private ProductImageContentSchema[] imageArray;

            @Schema(name = "商品圖片", description = "商品圖片")
            @Hidden
            @Data
            private static class ProductImageContentSchema {
                @Schema(description = "商品圖片編號", example = "1")
                private Integer id;
                @Schema(description = "商品圖片網址", example = "https://www.ntub.edu.tw/var/file/0/1000/img/1595/logo.png")
                private String url;
            }
        }
    }

    @Schema(name = "商品群組內容(編輯)", description = "商品群組內容(編輯)")
    @Data
    private static class UpdateProductGroupContentSchema {
        @Schema(description = "商品群組名稱", example = "便宜帳篷、桌椅三件套，限時特價$3990")
        private String name;
        @Schema(description = "城市", example = "台北市 中正區")
        private String city;
        @Schema(description = "封面圖", example = "https://www.ntub.edu.tw/var/file/0/1000/img/1595/logo.png")
        private String coverImage;
        @Schema(description = "租借價格", minimum = "0", example = "3990")
        private int price;
        @Schema(description = "可租借起始時間", example = "2020/08/14 15:00")
        private String borrowStartDate;
        @Schema(description = "可租借結束時間", example = "2020/08/20 00:00")
        private String borrowEndDate;
        @Schema(description = "出租者帳號", example = "admin")
        private String productOwnerAccount;
        @Schema(description = "出租者帳號(暱稱)", example = "admin(管理員)")
        private String productOwnerName;
        @Schema(description = "出租者銀行帳戶", example = "123456")
        private String bankAccount;
        @Schema(description = "聯絡方式", example = "10646007@ntub.edu.tw")
        private String contact;
        @Schema(description = "評價", example = "2.5")
        private Double comment;
        @ArraySchema(minItems = 0, uniqueItems = true, schema = @Schema(description = "商品陣列"))
        private ProductContentSchema productArray;

        @Hidden
        @Data
        private static class ProductContentSchema {
            @Schema(description = "商品編號", minimum = "1", example = "6")
            private Integer id;
            @Schema(description = "商品類型", example = "睡帳")
            private String type;
            @Schema(description = "商品名稱", example = "OO牌睡帳")
            private String name;
            @Schema(description = "商品數量", example = "2")
            private Integer count;
            @Schema(description = "商品品牌", example = "OO")
            private String brand;
            @Schema(description = "商品外觀狀況", example = "300cm*300cm*250cm(高)")
            private String appearance;
            @Schema(description = "使用說明", example = "內附搭帳棚說明書")
            private String useInformation;
            @Schema(description = "損壞賠償", example = "缺少零件：1/$200、布劃破：$1000")
            private String brokenCompensation;
            @Schema(description = "備註", nullable = true, example = "附有教學影片，若在搭設過程有疑問，都可以聯絡我")
            private String memo;
            @ArraySchema(minItems = 0, schema = @Schema(description = "商品圖片陣列", implementation = ProductImageContentSchema.class))
            private ProductImageContentSchema[] imageArray;

            @Schema(name = "商品圖片", description = "商品圖片")
            @Hidden
            @Data
            private static class ProductImageContentSchema {
                @Schema(description = "商品圖片編號", example = "1")
                private Integer id;
                @Schema(description = "商品圖片網址", example = "https://www.ntub.edu.tw/var/file/0/1000/img/1595/logo.png")
                private String url;
            }
        }
    }

    @Schema(name = "更新商品群組", description = "更新商品群組")
    @Data
    private static class UpdateProductGroupSchema {
        @Schema(description = "匯款帳戶", minLength = 10, maxLength = 16, example = "12345671234567")
        private String bankAccount;
        @Schema(description = "商品群組名稱", example = "便宜帳篷、桌椅三件套，限時特價$3990")
        private String name;
        @Schema(description = "封面圖連結", example = "https://www.ntub.edu.tw/var/file/0/1000/img/1595/logo.png")
        private String coverImage;
        @Schema(description = "城市名稱，如臺北市、宜蘭縣", example = "臺北市")
        private String cityName;
        @Schema(description = "區名稱，如中正區、宜蘭市", example = "中正區")
        private String cityAreaName;
        @Schema(description = "價格", example = "3990")
        private Integer price;
        @Schema(description = "租借起始日期(需在結束日期之前)", example = "2020/08/11")
        private LocalDateTime borrowStartDate;
        @Schema(description = "租借結束日期(需在起始日期之後)", example = "2020/08/12")
        private LocalDateTime borrowEndDate;
        @ArraySchema(minItems = 0, schema = @Schema(description = "商品", implementation = UpdateProductSchema.class))
        private List<UpdateProductSchema> productArray;
    }

    @Schema(name = "更新商品", description = "更新商品")
    @Data
    private static class UpdateProductSchema {
        @Schema(description = "商品類型", minimum = "1", example = "1")
        private Integer type;
        @Schema(description = "商品名稱", example = "OO牌帳篷")
        private String name;
        @Schema(description = "數量", minimum = "1", example = "2")
        private Integer count;
        @Schema(description = "品牌", example = "OO")
        private String brand;
        @Schema(description = "商品外觀狀況", example = "300cm*300cm*250cm(高)")
        private String appearance;
        @Schema(description = "使用方式", example = "內附搭帳篷說明書")
        private String useInformation;
        @Schema(description = "損壞賠償", example = "缺少零件：1/$200、布劃破：$1000")
        private String brokenCompensation;
        private String relatedLink;
        @Schema(description = "備註", example = "附有教學影片，若在搭設過程有疑問，都可以聯絡我")
        private String memo;
    }

    @Schema(name = "商品群組評價資料", description = "商品群組評價資料")
    @Data
    private static class ProductGroupCommentSchema {
        @Schema(description = "評價分數", minimum = "1", maximum = "5")
        private Byte comment;
    }

    @Schema(name = "商品子類型查詢資料", description = "商品子類型查詢資料")
    @Data
    private static class ProductSubTypeSchema {
        @Schema(description = "子類型編號", example = "1")
        private Integer id;
        @Schema(description = "子類型名稱", example = "小於四人帳篷")
        private String name;
    }

    @Schema(name = "建議售價查詢資料", description = "建議售價查詢資料")
    @Data
    private static class RecommendProductPriceSchema {
        @Schema(description = "建議售價", minimum = "0", example = "472")
        private Integer price;
    }
}
