package fansirsqi.xposed.sesame.rpc.debug;

import fansirsqi.xposed.sesame.hook.ApplicationHook;
import fansirsqi.xposed.sesame.task.antDodo.AntDodoRpcCall;
import fansirsqi.xposed.sesame.task.antForest.AntForestRpcCall;
import fansirsqi.xposed.sesame.task.antOrchard.AntOrchardRpcCall;
import fansirsqi.xposed.sesame.task.reserve.ReserveRpcCall;
import fansirsqi.xposed.sesame.util.LogUtil;
import fansirsqi.xposed.sesame.util.TimeUtil;
import fansirsqi.xposed.sesame.util.UserIdMapUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DebugRpc {
  private static final String TAG = DebugRpc.class.getCanonicalName();

  public String getName() {
    return "Rpc测试";
  }

  public  void start(String broadcastFun, String broadcastData, String testType) {
    new Thread() {
      String broadcastFun;
      String broadcastData;
      String testType;

      public Thread setData(String fun, String data, String type) {
        broadcastFun = fun;
        broadcastData = data;
        testType = type;
        return this;
      }

      @Override
      public void run() {
        switch (testType) {
          case "Rpc":
            String s = test(broadcastFun, broadcastData);
            LogUtil.forest("收到测试消息:\n方法:" + broadcastFun + "\n数据:" + broadcastData + "\n结果:" + s);
            break;
          case "getNewTreeItems": // 获取新树上苗🌱信息
            getNewTreeItems();
            break;
          case "getTreeItems": // 🔍查询树苗余量
            getTreeItems();
            break;

          case "walkGrid": // 走格子
            walkGrid();
            break;
          default:
            LogUtil.forest("未知的测试类型: " + testType);
            break;
        }
      }
    }.setData(broadcastFun, broadcastData, testType).start();
  }

  private String test(String fun, String data) {
    return ApplicationHook.requestString(fun, data);
  }


  public String queryEnvironmentCertDetailList(String alias, int pageNum, String targetUserID) {
    return DebugRpcCall.queryEnvironmentCertDetailList(alias, pageNum, targetUserID);
  }

  public String sendTree(String certificateId, String friendUserId) {
    return DebugRpcCall.sendTree(certificateId, friendUserId);
  }

  private void getNewTreeItems() {
    try {
      String s = ReserveRpcCall.queryTreeItemsForExchange();
      JSONObject jo = new JSONObject(s);
      if ("SUCCESS".equals(jo.getString("resultCode"))) {
        JSONArray ja = jo.getJSONArray("treeItems");
        for (int i = 0; i < ja.length(); i++) {
          jo = ja.getJSONObject(i);
          if (!jo.has("projectType")) continue;
          if (!"TREE".equals(jo.getString("projectType"))) continue;
          if (!"COMING".equals(jo.getString("applyAction"))) continue;
          String projectId = jo.getString("itemId");
          queryTreeForExchange(projectId);
        }
      } else {
        LogUtil.runtime(TAG, jo.getString("resultDesc"));
      }
    } catch (Throwable t) {
      LogUtil.runtime(TAG, "getTreeItems err:");
      LogUtil.printStackTrace(TAG, t);
    }
  }

  /**
   * 查询特定项目下可交换树木的信息。
   *
   * @param projectId 项目ID
   */
  private void queryTreeForExchange(String projectId) {
    try {
      // 调用RPC方法查询树木交换信息
      String response = ReserveRpcCall.queryTreeForExchange(projectId);
      JSONObject jo = new JSONObject(response);
      // 检查RPC调用结果码是否为"SUCCESS"，表示成功
      if ("SUCCESS".equals(jo.getString("resultCode"))) {
        // 获取可交换树木的信息
        JSONObject exchangeableTree = jo.getJSONObject("exchangeableTree");
        // 获取当前预算
        int currentBudget = exchangeableTree.getInt("currentBudget");
        // 获取区域信息
        String region = exchangeableTree.getString("region");
        // 获取树木名称
        String treeName = exchangeableTree.getString("treeName");
        // 默认提示信息为"不可合种"
        String tips = "不可合种";
        // 检查是否可以合种，如果可以，则更新提示信息
        if (exchangeableTree.optBoolean("canCoexchange", false)) {
          // 获取合种类型信息
          String coexchangeTypeIdList = exchangeableTree.getJSONObject("extendInfo").getString("cooperate_template_id_list");
          tips = "可以合种-合种类型：" + coexchangeTypeIdList;
        }
        // 记录查询结果
        LogUtil.record("新树上苗🌱[" + region + "-" + treeName + "]#" + currentBudget + "株-" + tips);
      } else {
        // 如果RPC调用失败，记录错误描述和项目ID
        // 注意：这里应该记录projectId而不是s（响应字符串）
        LogUtil.record(jo.getString("resultDesc") + " projectId: " + projectId);
      }
    } catch (JSONException e) {
      // 处理JSON解析异常
      LogUtil.runtime(TAG, "JSON解析错误:");
      LogUtil.printStackTrace(TAG, e);
    } catch (Throwable t) {
      // 处理其他可能的异常
      LogUtil.runtime(TAG, "查询树木交换信息过程中发生错误:");
      LogUtil.printStackTrace(TAG, t);
    }
  }



  /**
   * 获取可交换的树木项目列表，并对每个可用的项目查询当前预算。
   */
  private void getTreeItems() {
    try {
      // 调用RPC方法查询可交换的树木项目列表
      String response = ReserveRpcCall.queryTreeItemsForExchange();
      JSONObject jo = new JSONObject(response);
      // 检查RPC调用结果码是否为"SUCCESS"，表示成功
      if ("SUCCESS".equals(jo.getString("resultCode"))) {
        // 获取树木项目列表
        JSONArray ja = jo.getJSONArray("treeItems");
        // 遍历项目列表
        for (int i = 0; i < ja.length(); i++) {
          // 获取单个项目信息
          jo = ja.getJSONObject(i);
          // 如果项目信息中不包含"projectType"字段，则跳过当前项目
          if (!jo.has("projectType")) continue;
          // 如果项目的应用操作不是"AVAILABLE"，则跳过当前项目
          if (!"AVAILABLE".equals(jo.getString("applyAction"))) continue;
          // 获取项目ID和项目名称
          String projectId = jo.getString("itemId");
          String itemName = jo.getString("itemName");
          // 对当前项目查询当前预算
          getTreeCurrentBudget(projectId, itemName);

          // 在查询每个项目后暂停100毫秒
          TimeUtil.sleep(100);
        }
      } else {
        // 如果RPC调用失败，记录错误描述
        LogUtil.runtime(TAG, jo.getString("resultDesc"));
      }
    } catch (JSONException e) {
      // 处理JSON解析异常
      LogUtil.runtime(TAG, "JSON解析错误:");
      LogUtil.printStackTrace(TAG, e);
    } catch (Throwable t) {
      // 处理其他可能的异常
      LogUtil.runtime(TAG, "获取树木项目列表过程中发生错误:");
      LogUtil.printStackTrace(TAG, t);
    }
  }


  /**
   * 树苗查询
   *
   * @param projectId 项目ID
   * @param treeName  树木名称
   */
  private void getTreeCurrentBudget(String projectId, String treeName) {
    try {
      // 调用RPC方法查询树木交换信息
      String response = ReserveRpcCall.queryTreeForExchange(projectId);
      JSONObject jo = new JSONObject(response);

      // 检查RPC调用结果码是否为"SUCCESS"，表示成功
      if ("SUCCESS".equals(jo.getString("resultCode"))) {
        // 获取可交换树木的信息
        JSONObject exchangeableTree = jo.getJSONObject("exchangeableTree");
        // 获取当前预算
        int currentBudget = exchangeableTree.getInt("currentBudget");
        // 获取区域信息
        String region = exchangeableTree.getString("region");

        // 记录树木查询结果
        LogUtil.record("树苗查询🌱[" + region + "-" + treeName + "]#剩余:" + currentBudget);
      } else {
        // 如果RPC调用失败，记录错误描述和项目ID
        LogUtil.record(jo.getString("resultDesc") + " projectId: " + projectId);
      }
    } catch (JSONException e) {
      // 处理JSON解析异常
      LogUtil.runtime(TAG, "JSON解析错误:");
      LogUtil.printStackTrace(TAG, e);
    } catch (Throwable t) {
      // 处理其他可能的异常
      LogUtil.runtime(TAG, "查询树木交换信息过程中发生错误:");
      LogUtil.printStackTrace(TAG, t);
    }
  }


  /**
   * 模拟网格行走过程，处理行走中的事件，如完成迷你游戏和广告任务。
   */
  private void walkGrid() {
    try {
      // 调用RPC方法模拟网格行走
      String s = DebugRpcCall.walkGrid();
      JSONObject jo = new JSONObject(s);
      // 检查RPC调用是否成功
      if (jo.getBoolean("success")) {
        JSONObject data = jo.getJSONObject("data");
        // 检查是否有地图奖励
        if (!data.has("mapAwards")) return;
        JSONArray mapAwards = data.getJSONArray("mapAwards");
        JSONObject mapAward = mapAwards.getJSONObject(0);
        // 检查是否有迷你游戏信息
        if (mapAward.has("miniGameInfo")) {
          JSONObject miniGameInfo = mapAward.getJSONObject("miniGameInfo");
          String gameId = miniGameInfo.getString("gameId");
          String key = miniGameInfo.getString("key");
          // 模拟等待迷你游戏完成
          Thread.sleep(4000L);
          // 调用RPC方法完成迷你游戏
          jo = new JSONObject(DebugRpcCall.miniGameFinish(gameId, key));
          // 检查迷你游戏是否完成成功
          if (jo.getBoolean("success")) {
            JSONObject miniGamedata = jo.getJSONObject("data");

            // 检查是否有广告任务信息
            if (miniGamedata.has("adVO")) {
              JSONObject adVO = miniGamedata.getJSONObject("adVO");

              // 检查是否有广告业务编号
              if (adVO.has("adBizNo")) {
                String adBizNo = adVO.getString("adBizNo");

                // 调用RPC方法完成广告任务
                jo = new JSONObject(DebugRpcCall.taskFinish(adBizNo));

                // 检查广告任务是否完成成功
                if (jo.getBoolean("success")) {
                  // 查询广告任务是否真的完成
                  jo = new JSONObject(DebugRpcCall.queryAdFinished(adBizNo, "NEVERLAND_DOUBLE_AWARD_AD"));

                  // 检查查询结果是否成功
                  if (jo.getBoolean("success")) {
                    LogUtil.farm("完成双倍奖励🎁");
                  }
                }
              }
            }
          }
        }

        // 获取剩余行走次数
        int leftCount = data.getInt("leftCount");

        // 如果还有剩余次数，继续行走
        if (leftCount > 0) {
          Thread.sleep(3000L);
          walkGrid(); // 递归调用，继续行走
        }
      } else {
        // 如果RPC调用失败，记录错误信息
        LogUtil.record(jo.getString("errorMsg") + s);
      }
    } catch (JSONException e) {
      // 处理JSON解析异常
      LogUtil.runtime(TAG, "JSON解析错误:");
      LogUtil.printStackTrace(TAG, e);
    } catch (InterruptedException e) {
      // 处理线程中断异常
      LogUtil.runtime(TAG, "线程中断错误:");
      LogUtil.printStackTrace(TAG, e);
    } catch (Throwable t) {
      // 处理其他可能的异常
      LogUtil.runtime(TAG, "行走网格过程中发生错误:");
      LogUtil.printStackTrace(TAG, t);
    }
  }

}