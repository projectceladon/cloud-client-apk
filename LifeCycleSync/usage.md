start app：
adb shell am start  lifecyclesyc/lifecyclesyc.MainActivity

close app：
adb shell pm clear lifecyclesyc
or
adb shell am force-stop lifecyclesyc

adb shell input keyevent 4 && pm clear lifecyclesyc

查看时候运行：
adb shell ps | grep lifecyclesyc