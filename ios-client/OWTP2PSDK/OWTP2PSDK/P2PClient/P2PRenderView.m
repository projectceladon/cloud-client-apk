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

#import "P2PRenderView.h"

@interface P2PRenderView()
@property (strong, nonatomic) void (^touchCallback)(int state, int index, float x, float y);
@end
@implementation P2PRenderView

- (void)setTouch:(void (^)(int state, int index, float x, float y))block {
    self.touchCallback = block;
}

- (void)touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event {
    
    for (int i = 0; i < touches.count; ++i) {
        UITouch* touch = touches.allObjects[i];
        CGPoint point = [touch locationInView:self];
        if (self.touchCallback)
            self.touchCallback(0, i, point.x / self.frame.size.width, point.y / self.frame.size.height);
    }
    
    [super touchesBegan:touches withEvent:event];
}

- (void)touchesEnded:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    
    for (int i = 0; i < touches.count; ++i) {
        UITouch* touch = touches.allObjects[i];
        CGPoint point = [touch locationInView:self];
        if (self.touchCallback)
            self.touchCallback(2, i, point.x / self.frame.size.width, point.y / self.frame.size.height);
    }
    
    [super touchesEnded:touches withEvent:event];
}

- (void)touchesMoved:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    
    for (int i = 0; i < touches.count; ++i) {
        UITouch* touch = touches.allObjects[i];
        CGPoint point = [touch locationInView:self];
        if (self.touchCallback)
            self.touchCallback(1, i, point.x / self.frame.size.width, point.y / self.frame.size.height);
    }
    
    [super touchesMoved:touches withEvent:event];
}

- (void)touchesCancelled:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    
    for (int i = 0; i < touches.count; ++i) {
        UITouch* touch = touches.allObjects[i];
        CGPoint point = [touch locationInView:self];
        if (self.touchCallback)
            self.touchCallback(3, i, point.x / self.frame.size.width, point.y / self.frame.size.height);
    }
    
    [super touchesCancelled:touches withEvent:event];
}

@end
