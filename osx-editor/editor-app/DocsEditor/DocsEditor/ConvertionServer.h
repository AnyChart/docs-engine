//
//  ConvertionServer.h
//  DocsEditor
//
//  Created by Aleksandr Batsuev on 17/03/14.
//  Copyright (c) 2014 Aleksandr Batsuev. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface ConvertionServer : NSObject

+ (ConvertionServer*)server;

@property NSString* port;
@property (readonly) bool serverAvailable;

@end
