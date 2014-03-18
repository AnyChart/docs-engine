//
//  ConvertionServer.m
//  DocsEditor
//
//  Created by Aleksandr Batsuev on 17/03/14.
//  Copyright (c) 2014 Aleksandr Batsuev. All rights reserved.
//

#import "ConvertionServer.h"
#import <AFNetworking.h>

@interface ConvertionServer()

@property NSTask* converter;
@property NSFileHandle* outFile;

@end

@implementation ConvertionServer

+ (ConvertionServer*)server {
    static ConvertionServer *sharedMyManager = nil;
    @synchronized(self) {
        if (sharedMyManager == nil)
            sharedMyManager = [[self alloc] init];
    }
    return sharedMyManager;
}

- (id)init {
    self = [super init];
    if (self) {
        _serverAvailable = NO;
        [self start];
    }
    return self;
}

- (void)start {
    NSLog(@"start");
    self.converter = [[NSTask alloc] init];
    [self.converter setLaunchPath:@"/usr/bin/java"];
    [self.converter setArguments:[NSArray arrayWithObjects:
                                  @"-jar",
                                  [[NSBundle mainBundle] pathForResource:@"editor_server-0.1.0-SNAPSHOT-standalone"
                                                                  ofType:@"jar"], nil]];
    NSPipe * out = [NSPipe pipe];
    [self.converter setStandardOutput:out];
    [self.converter launch];
    
    self.outFile = [out fileHandleForReading];
    [self.outFile waitForDataInBackgroundAndNotify];
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(commandNotification:)
                                                 name:NSFileHandleDataAvailableNotification
                                               object:nil];
}

- (void)commandNotification:(NSNotification *)notification
{
    if ([notification.name isEqualToString:@"NSFileHandleDataAvailableNotification"]) {
        NSData *data = [self.outFile availableData];
        self.port = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
        self.port = [self.port stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceAndNewlineCharacterSet]];
        [self.outFile closeFile];
        self.outFile = nil;
        
        [[NSNotificationCenter defaultCenter] removeObserver:self name:NSFileHandleDataAvailableNotification object:nil];
        
        [self checkServerAvailable];
    }
}

- (void)checkServerAvailable {
    AFHTTPRequestOperationManager *manager = [AFHTTPRequestOperationManager manager];
    manager.responseSerializer = [AFHTTPResponseSerializer serializer];
    NSDictionary *parameters = @{@"data": @"" };
    NSString *url = [NSString stringWithFormat:@"http://127.0.0.1:%@/", self.port];
    NSLog(@"Check server");
    [manager POST:url parameters:parameters
          success:^(AFHTTPRequestOperation *operation, id responseObject) {
              [self onAvailable];
          } failure:^(AFHTTPRequestOperation *operation, NSError *error) {
              [self checkServerAvailable];
          }];
}

- (void)onAvailable {
    _serverAvailable = YES;
    [[NSNotificationCenter defaultCenter] postNotificationName:@"ServerAvailable" object:self];
}

@end
