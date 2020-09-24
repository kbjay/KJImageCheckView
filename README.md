# 仿微信图片查看器
1. 双指中心放缩 
2. 双击根据点击位置放缩 
3. 拖动查看图片（包括fling处理）
4. 图片“初始尺寸”时可以下拉隐藏

# 使用
1. 自定义属性如下:

    ```xml
    <!--  fling 的overX跟overY  -->
    <attr name="span_over_fling" format="integer" />
    <!--  图片是”原始“尺寸时,下滑多少距离触发dismiss动画  -->
    <attr name="dismiss_translate_y_span" format="integer" />
    <!--  图片放大的最大倍数（相对于”原始“尺寸）  -->
    <attr name="max_scale_rate" format="float" />
    <!--  双指放大是回弹的factor  -->
    <attr name="scale_rebound_factor" format="float" />
    <!--  下滑消失开关  -->
    <attr name="enable_scroll_dismiss" format="boolean" />
    ```

2. api
    1. 设置bitmap
    
       ```kotlin
       kjView.showBitmap(resource)
       ```
    
       
    
    2. 设置dismiss监听
    
       ```kotlin
       kjView.mDismissListener = object : KJScrollScaleImageView.DismissListener {
           override fun onDismissFinish() {
               TODO("Not yet implemented")
           }
       
           override fun onDismissStart() {
               TODO("Not yet implemented")
           }
       }
       ```
    
       
    

# 下载
![apk下载](https://github.com/kbjay/KJImageCheckView/blob/master/apk.png)
