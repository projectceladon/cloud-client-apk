/*
 * Copyright © 2021 Intel Corporation. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#import "P2PClient.h"
#import "P2PSignalingChannel.h"
#import "P2PRenderView.h"
#define WeakSelf __weak typeof(self) weakSelf = self;

#define START @"start"
#define STUN @"stun:%@:3478"
#define TURN_TCP @"turn:%@:3478?transport=tcp"
#define TURN_UDP @"turn:%@:3478?transport=udp"

@interface P2PClient () <OWTP2PClientDelegate, RTCVideoViewDelegate, OWTRemoteStreamDelegate, OWTP2PPublicationDelegate>

@property (strong, nonatomic) OWTP2PClient* peerClient;
@property (strong, nonatomic) P2PRenderView* remoteVideoView;
@property (strong, nonatomic) OWTLocalStream* localVideoStream;
@property (strong, nonatomic) OWTP2PPublication* videoPublication;
@property (strong, nonatomic) NSString* peerId;

@end

@implementation P2PClient

- (void) initP2PClient:(NSString*) ip {
    
    id<OWTP2PSignalingChannelProtocol> scc = [[P2PSignalingChannel alloc] init];
    OWTP2PClientConfiguration* config = [[OWTP2PClientConfiguration alloc] init];
    OWTAudioCodecParameters* opusParameters = [[OWTAudioCodecParameters alloc] init];
    opusParameters.name = OWTAudioCodecOpus;
    OWTAudioEncodingParameters *audioParameters = [[OWTAudioEncodingParameters alloc] init];
    audioParameters.codec = opusParameters;
    config.audio = @[audioParameters];
    
    OWTVideoCodecParameters *h264Parameters = [[OWTVideoCodecParameters alloc] init];
    h264Parameters.name = OWTVideoCodecH264;
    OWTVideoEncodingParameters *videoH264Parameters = [[OWTVideoEncodingParameters alloc] init];
    videoH264Parameters.codec = h264Parameters;
    
    OWTVideoCodecParameters *h265Parameters = [[OWTVideoCodecParameters alloc] init];
    h265Parameters.name = OWTVideoCodecH265;
    OWTVideoEncodingParameters *videoH265Parameters = [[OWTVideoEncodingParameters alloc] init];
    videoH265Parameters.codec = h265Parameters;
    
    config.video = @[videoH264Parameters, videoH265Parameters];
    config.rtcConfiguration = [[RTCConfiguration alloc] init];
    
    RTCIceServer* server = [[RTCIceServer alloc] initWithURLStrings:@[ [[NSString alloc] initWithFormat:STUN, ip],
                                                                       [[NSString alloc] initWithFormat:TURN_TCP, ip],
                                                                       [[NSString alloc] initWithFormat:TURN_UDP, ip]]
                                                           username:@"username" credential:@"password"];
    
    config.rtcConfiguration.iceServers = @[server];
    
    self.peerClient = [[OWTP2PClient alloc] initWithConfiguration:config signalingChannel:scc];
    self.peerClient.delegate = self;
}

- (void)setRenderView:(UIView*) renderView {
    if (renderView == nil) {
        if (self.remoteVideoView) {
            [self.remoteVideoView setTouch: nil];
            [self.remoteVideoView removeFromSuperview];
        }
        return;
    }
    
    if (self.remoteVideoView == nil) {
        self.remoteVideoView = [[P2PRenderView alloc] initWithFrame:CGRectMake(0, 0, renderView.bounds.size.width, renderView.bounds.size.height)];
        self.remoteVideoView.delegate = self;
        [renderView addSubview:self.remoteVideoView];
    } else {
        [self.remoteVideoView removeFromSuperview];
        [self.remoteVideoView setFrame:CGRectMake(0, 0, renderView.bounds.size.width, renderView.bounds.size.height)];
        self.remoteVideoView.delegate = self;
        [renderView addSubview:self.remoteVideoView];
    }
    
    WeakSelf
    [self.remoteVideoView setTouch:^(int state, int index, float x, float y) {
        if (weakSelf.peerId) {
            
            NSString* stateString;
            NSString* posString = @"";
            switch (state) {
                case 0:
                    stateString = @"d ";
                    posString = [[NSString alloc] initWithFormat:@" %d %d 255", (int)(32767 * x), (int)(32767 * y)];
                    break;
                case 1:
                    stateString = @"m ";
                    posString = [[NSString alloc] initWithFormat:@" %d %d 255", (int)(32767 * x), (int)(32767 * y)];
                    break;
                case 2:
                case 3:
                default:
                    stateString = @"u ";
                    break;
            }
            
            // TODO: json transfer from model
            NSString* rawDataString = [[NSString alloc] initWithFormat:@"{\"data\":{\"event\":\"touch\",\"parameters\":{\"action\":0,\"data\":\"%@%d%@\\nc\\n\",\"fingerId\":0,\"jID\":0,\"keycode\":0,\"tID\":0,\"touchx\":0.0,\"touchy\":0.0}},\"type\":\"control\"}", stateString, index, posString];
            
            [weakSelf.peerClient send:rawDataString to:weakSelf.peerId  onSuccess:^{
                NSLog(@"send success");
            } onFailure:^(NSError * _Nonnull error) {
                NSLog(@"send fail");
            }];
        }
    }];
}

- (void)p2pClientDidDisconnect:(OWTP2PClient*)client {
    NSLog(@">> p2pClientDidDisconnect");
}

- (void)p2pClient:(OWTP2PClient*)client didAddStream:(OWTRemoteStream*)stream {
    if (stream) {
        [stream attach: self.remoteVideoView];
        stream.delegate = self;
    } else {
        NSLog(@">> stream is nil");
        abort();
    }
}

- (void)p2pClient:(OWTP2PClient*)client didReceiveMessage:(NSString*)message from:(NSString*)senderId {
    NSLog(@">> Recieved data from %@, message: %@", senderId, message);
    NSData *data = [message dataUsingEncoding:NSUTF8StringEncoding];
    NSDictionary* jd = [NSJSONSerialization JSONObjectWithData:data options:0 error:nil];
    NSString * key = [jd objectForKey:@"key"];
    
    if([key isEqualToString:@"start-camera-preview"]) {
        NSLog(@">> start-camera-preview ====");
        [self publishLocalVideoStream];
    } else if ([key isEqualToString:@"stop-camera-preview"]){
        NSLog(@">> stop-camera-preview ===== ");
        [self destroyPublishLocalStream];
    }
    
}

- (void)publishLocalVideoStream {
    OWTStreamConstraints *constraints=[[OWTStreamConstraints alloc] init];
    constraints.audio = NO;
    constraints.video = [[OWTVideoTrackConstraints alloc] init];
    constraints.video.frameRate = 24;
    constraints.video.resolution= CGSizeMake(640, 480);
    constraints.video.devicePosition = AVCaptureDevicePositionFront;
    NSError* error = nil;
    
    WeakSelf
    self.localVideoStream = [[OWTLocalStream alloc] initWithConstratins:constraints error:&error];
    [self.peerClient publish:self.localVideoStream to:self.peerId onSuccess:^(OWTP2PPublication * publication) {
        NSLog(@">> publish success");
        weakSelf.videoPublication = publication;
        weakSelf.videoPublication.delegate = weakSelf;
    } onFailure:^(NSError * error) {
        NSLog(@">> %@",error);
    }];
}

- (void)destroyPublishLocalStream {
    if (self.videoPublication && self.localVideoStream) {
        [self.videoPublication stop];
        RTCCameraVideoCapturer* cameraVideoCapturer = [self.localVideoStream valueForKey:@"capturer"];
        if (cameraVideoCapturer && [cameraVideoCapturer isKindOfClass:RTCCameraVideoCapturer.class]) {
            [cameraVideoCapturer stopCapture];
        }
        self.localVideoStream = nil;
    }
}

- (void) connect:(NSString*) host peerId:(NSString*)peer clientId:(NSString*)client block:(void (^)(BOOL success))block {
    
    NSString* ip;
    if ([host hasPrefix:@"http://"] ) {
        NSArray *strArr = [host componentsSeparatedByString:@"//"];
        if (strArr.count == 2) {
            NSString* endpart = strArr[1];
            ip = [endpart componentsSeparatedByString:@":"][0];
        } else {
            NSLog(@"host invalid");
            block(NO);
            return;
        }
    } else {
        NSLog(@"host should start with 'http://'");
        block(NO);
        return;
    }
    
    if (self.peerClient == NULL) {
        [self initP2PClient: ip];
    }
    
    self.peerId = peer;
    
    NSMutableDictionary *tokenDict = [[NSMutableDictionary alloc]init];
    [tokenDict setValue:host forKey:@"host"];
    [tokenDict setValue:client forKey:@"token"];
    
    NSError* error;
    NSData* tokenData = [NSJSONSerialization dataWithJSONObject:tokenDict options:NSJSONWritingWithoutEscapingSlashes error:&error];
    NSString *tokenString = [[NSString alloc]initWithData:tokenData encoding:NSUTF8StringEncoding];
    
    WeakSelf
    [self.peerClient connect:tokenString onSuccess:^(NSString *msg) {
        [weakSelf.peerClient setAllowedRemoteIds:[[NSMutableArray alloc] initWithArray:@[peer]]];
        [weakSelf.peerClient stop:peer];
        [weakSelf.peerClient send:START to:peer onSuccess:^{
            dispatch_async(dispatch_get_main_queue(), ^{
                block(YES);
            });
        } onFailure:^(NSError * _Nonnull error) {
            dispatch_async(dispatch_get_main_queue(), ^{
                block(NO);
            });
        }];
        
    } onFailure:^(NSError * _Nonnull err){
        dispatch_async(dispatch_get_main_queue(), ^{
            block(NO);
        });
    }];
}

- (void) disconnect:(void (^)(BOOL success))block {
    if (self.peerId) {
        [self.peerClient disconnectWithOnSuccess:^{
            dispatch_async(dispatch_get_main_queue(), ^{
                if (block)
                    block(YES);
            });
        } onFailure:^(NSError * _Nonnull error) {
            dispatch_async(dispatch_get_main_queue(), ^{
                if (block)
                    block(NO);
            });
        }];
    }
}

- (void)videoView:(nonnull id<RTCVideoRenderer>)videoView didChangeVideoSize:(CGSize)size {
    NSLog(@"didChangeVideoSize");
}

- (void)streamDidEnd:(OWTRemoteStream *)stream {
    NSLog(@"stream did end");
}

- (void)streamDidMute:(nonnull OWTRemoteStream *)stream trackKind:(OWTTrackKind)kind {
    
}


- (void)streamDidUnmute:(nonnull OWTRemoteStream *)stream trackKind:(OWTTrackKind)kind {
    
}


- (void)streamDidUpdate:(nonnull OWTRemoteStream *)stream {
    
}


@end
