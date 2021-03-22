mkdir -p /tmp/codechal_dump
rm /tmp/codechal_dump/*
cp buffer_config.txt /tmp/codechal_dump/
./bin/mv_decoder_adv -i output.264 --avc --limit 1000 --long
./avc_streamout_demo --dump

echo ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>"
echo "check streamout buffer in /tmp/codechal_dump/dump_streamout_buffer_surface_1.txt ... "
echo ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>"
