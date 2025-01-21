package fansirsqi.xposed.sesame.task.antForest;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import fansirsqi.xposed.sesame.entity.VitalityStore.ExchangeStatus;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.Maps.IdMapManager;
import fansirsqi.xposed.sesame.util.Maps.UserMap;
import fansirsqi.xposed.sesame.util.Maps.VitalityRewardsMap;
import fansirsqi.xposed.sesame.util.ResUtil;
import fansirsqi.xposed.sesame.util.StatusUtil;

/**
 * @author Byseven
 * @see  2025/1/20
 * @apiNote
 */
public class Vitality {

    private static final String TAG = Vitality.class.getSimpleName();

    static Map<String, JSONObject> skuInfo = new HashMap<>();

    public static JSONArray ItemListByType(String labelType) {
        JSONArray itemInfoVOList = null;
        try {
            JSONObject jo = new JSONObject(AntForestRpcCall.itemList(labelType));
            if (ResUtil.checkSuccess(TAG, jo)) {
                itemInfoVOList = jo.optJSONArray("itemInfoVOList");
            }
        } catch (Throwable th) {
            Log.runtime(TAG, "ItemListByType err");
            Log.printStackTrace(TAG, th);
        }
        return itemInfoVOList;
    }

    public static void ItemDetailBySpuId(String spuId) {
        try {
            JSONObject jo = new JSONObject(AntForestRpcCall.itemDetail(spuId));
            if (ResUtil.checkSuccess(TAG, jo)) {
                JSONObject ItemDetail = jo.getJSONObject("spuItemInfoVO");
                handleItemDetail(ItemDetail);
            }
        } catch (Throwable th) {
            Log.runtime(TAG, "ItemDetailBySpuId err");
            Log.printStackTrace(TAG, th);
        }
    }

    public static void initVitality(String labelType) {
        try {
            JSONArray itemInfoVOList = ItemListByType(labelType);
            if (itemInfoVOList != null) {
                for (int i = 0; i < itemInfoVOList.length(); i++) {
                    JSONObject itemInfoVO = itemInfoVOList.getJSONObject(i);
                    handleVitalityItem(itemInfoVO);
                }
            } else {
                Log.error(TAG, "活力兑换💱初始化失败！");
            }
        } catch (Throwable th) {
            Log.runtime(TAG, "initVitality err");
            Log.printStackTrace(TAG, th);
        }
    }


    private static void handleVitalityItem(JSONObject vitalityItem) {
        try {
            String spuId;
            JSONArray skuModelList = vitalityItem.getJSONArray("skuModelList");
            for (int i = 0; i < skuModelList.length(); i++) {
                JSONObject skuModel = skuModelList.getJSONObject(i);
                String skuId = skuModel.getString("skuId");
                spuId = skuModel.getString("spuId");//update spuId
                String skuName = skuModel.getString("skuName");
                if (!skuModel.has("spuId")) {
                    skuModel.put("spuId", spuId);
                }
                skuInfo.put(skuId, skuModel);
                IdMapManager.getInstance(VitalityRewardsMap.class).add(skuId, skuName);
            }
            IdMapManager.getInstance(VitalityRewardsMap.class).save(UserMap.getCurrentUid());
        } catch (Throwable th) {
            Log.runtime(TAG, "handleVitalityItem err");
            Log.printStackTrace(TAG, th);
        }
    }

    private static void handleItemDetail(JSONObject ItemDetail) {
        try {
            String spuId = ItemDetail.getString("spuId");
            JSONArray skuModelList = ItemDetail.getJSONArray("skuModelList");
            for (int i = 0; i < skuModelList.length(); i++) {
                JSONObject skuModel = skuModelList.getJSONObject(i);
                String skuId = skuModel.getString("skuId");
                String skuName = skuModel.getString("skuName");
                if (!skuModel.has("spuId")) {
                    skuModel.put("spuId", spuId);
                }
                skuInfo.put(skuId, skuModel);
                IdMapManager.getInstance(VitalityRewardsMap.class).add(skuId, skuName);
            }
            IdMapManager.getInstance(VitalityRewardsMap.class).save(UserMap.getCurrentUid());
        } catch (Throwable th) {
            Log.runtime(TAG, "handleItemDetail err:");
            Log.printStackTrace(TAG, th);
        }
    }



    /*
     * 兑换活力值商品
     * sku
     * spuId, skuId, skuName, exchangedCount, price[amount]
     * exchangedCount == 0......
     */
    public static Boolean handleVitalityExchange(String skuId) {
        if (skuInfo.isEmpty()) {
            initVitality("SC_ASSETS");
        }
        JSONObject sku = skuInfo.get(skuId);
        if (sku == null) {
            Log.record("活力兑换💱找不到要兑换的权益！");
            return false;
        }
        try {
            String skuName = sku.getString("skuName");

            JSONArray itemStatusList = sku.getJSONArray("itemStatusList");
            for (int i = 0; i < itemStatusList.length(); i++) {
                String itemStatus = itemStatusList.getString(i);
                ExchangeStatus Status = ExchangeStatus.valueOf(itemStatus);
                if (Status.name().equals(itemStatus) || Status.name().equals(itemStatus) || Status.name().equals(itemStatus)) {
                    Log.record("活力兑换💱[" + skuName + "]停止:" + Status.name());
                    if (ExchangeStatus.ExceedLimit.name().equals(itemStatus)) {
                        StatusUtil.setFlagToday("forest::VitalityExchangeLimit::" + skuId);
                        Log.forest("活力兑换💱[" + skuName + "]已达上限,停止兑换！");
                    }
                    return false;
                }
            }
            String spuId = sku.getString("spuId");
            if (VitalityExchange(spuId, skuId, skuName)) {
                if (skuName.contains("限时")){
                    StatusUtil.setFlagToday("forest::VitalityExchangeLimit::" + skuId);
                }
                return true;
            }
            ItemDetailBySpuId(spuId);
        } catch (Throwable th) {
            Log.runtime(TAG, "VitalityExchange err");
            Log.printStackTrace(TAG, th);
        }
        return false;
    }

    public static Boolean VitalityExchange(String spuId, String skuId, String skuName) {
        try {
            if (VitalityExchange(spuId, skuId)) {
                StatusUtil.vitalityExchangeToday(skuId);
                int exchangedCount = StatusUtil.getVitalityCount(skuId);
                Log.forest("活力兑换💱[" + skuName + "]#第" + exchangedCount + "次");
                return true;
            }
        } catch (Throwable th) {
            Log.runtime(TAG, "VitalityExchange err:" + spuId + "," + skuId);
            Log.printStackTrace(TAG, th);
        }
        return false;
    }

    private static Boolean VitalityExchange(String spuId, String skuId) {
        try {
            JSONObject jo = new JSONObject(AntForestRpcCall.exchangeBenefit(spuId, skuId));
            return ResUtil.checkResCode(TAG, jo);
        } catch (Throwable th) {
            Log.runtime(TAG, "VitalityExchange err:" + spuId + "," + skuId);
            Log.printStackTrace(TAG, th);
        }
        return false;
    }

    /**
     * 查找商店道具
     *
     * @param spuName xxx
     */
    public static JSONObject findSkuInfoBySkuName(String spuName) {
        try {
            if (skuInfo.isEmpty()) {
                initVitality("SC_ASSETS");
            }
            for (String key : skuInfo.keySet()) {
                JSONObject sku = skuInfo.get(key);
                assert sku != null;
                if(sku.getString("skuName").contains(spuName)){
                    return sku;
                }
            }
        } catch (Exception e) {
            Log.runtime(TAG, "findSkuInfoBySkuName err:");
            Log.printStackTrace(TAG, e);
        }
        return null;
    }
}
