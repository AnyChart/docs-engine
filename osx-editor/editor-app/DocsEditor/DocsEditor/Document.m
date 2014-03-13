//
//  Document.m
//  DocsEditor
//
//  Created by Aleksandr Batsuev on 11/03/14.
//  Copyright (c) 2014 Aleksandr Batsuev. All rights reserved.
//

#import "Document.h"
#import <AFNetworking.h>

@interface Document()

@property NSString* loadedDocument;
@property NSString* tempFilePath;
@property NSTask* converter;
@property NSString* port;
@property NSFileHandle* outFile;

@end

@implementation Document

- (id)init
{
    self = [super init];
    if (self) {
        // Add your subclass-specific initialization here.
    }
    return self;
}

- (NSString *)windowNibName
{
    // Override returning the nib file name of the document
    // If you need to use a subclass of NSWindowController or if your document supports multiple NSWindowControllers, you should remove this method and override -makeWindowControllers instead.
    return @"Document";
}

- (void)windowControllerDidLoadNib:(NSWindowController *)aController
{
    [super windowControllerDidLoadNib:aController];
        
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
    
    self.codeView.delegate = self;
    [self.codeView setMode:ACEModeMarkdown];
    [self.codeView setTheme:ACEThemeTextmate];
    [self.codeView setShowInvisibles:NO];
    
    [self.loadingView startAnimation:nil];
    
    self.tempFilePath = [NSTemporaryDirectory() stringByAppendingPathComponent: [NSString stringWithFormat: @"%.0f.", [NSDate timeIntervalSinceReferenceDate] * 1000.0]];
    
    if (self.loadedDocument != nil) {
        self.codeView.string = self.loadedDocument;
    }

    // Add any code here that needs to be executed once the windowController has loaded the document's window.
}

- (void)commandNotification:(NSNotification *)notification
{
    if ([notification.name isEqualToString:@"NSFileHandleDataAvailableNotification"]) {
        NSData *data = [self.outFile availableData];
        self.port = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
        self.port = [self.port stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceAndNewlineCharacterSet]];
        [self.outFile closeFile];
        self.outFile = nil;
        
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
    [self.loadingView stopAnimation:nil];
    [self.loadingView setHidden:YES];
    [self updatePreview];
}


- (void)updatePreview {
    if (self.port == nil) return;
    
    AFHTTPRequestOperationManager *manager = [AFHTTPRequestOperationManager manager];
    manager.responseSerializer = [AFHTTPResponseSerializer serializer];
    NSDictionary *parameters = @{@"data": self.codeView.string };
    NSString *url = [NSString stringWithFormat:@"http://127.0.0.1:%@/", self.port];
    
    [manager POST:url parameters:parameters
          success:^(AFHTTPRequestOperation *operation, id responseObject) {
              NSString *string = [[NSString alloc] initWithData:responseObject encoding:NSUTF8StringEncoding];
              
              string = [NSString stringWithFormat:@"<!doctype html>\
                <html lang='en'>\
                <head>\
                    <meta charset='utf-8'>\
                    <meta http-equiv='X-UA-Compatible' content='IE=edge'>\
                    <meta name='viewport' content='width=device-width, initial-scale=1'>\
                    <title>anychart documentation</title>\
                    <!-- Bootstrap -->\
                    <link href='http://docs.anychart.com/bootstrap/css/bootstrap.min.css' rel='stylesheet'>\
                    <script src='http://docs.anychart.com/jquery/jquery.min.js'></script>\
                    <script src='http://docs.anychart.com/bootstrap/js/bootstrap.min.js'></script>\
                        <link href='http://docs.anychart.com/css/docs.css' rel='stylesheet'></head><body><div class='container'>%@</div></body></html>", string];
              
              [self.preview.mainFrame loadHTMLString:string baseURL:nil];
    } failure:^(AFHTTPRequestOperation *operation, NSError *error) {
        NSLog(@"Error: %@", error);
    }];
}

- (void) textDidChange:(NSNotification *)notification {
    if ([notification.name isEqualToString:@"ACETextDidEndEditingNotification"])
        [self updatePreview];
}

+ (BOOL)autosavesInPlace
{
    return YES;
}

- (NSData *)dataOfType:(NSString *)typeName error:(NSError **)outError
{
    return [self.codeView.string dataUsingEncoding:NSUTF8StringEncoding];
}

- (BOOL)readFromData:(NSData *)data ofType:(NSString *)typeName error:(NSError **)outError
{
    self.loadedDocument = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
    return YES;
}
@end
