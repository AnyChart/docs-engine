//
//  Document.m
//  DocsEditor
//
//  Created by Aleksandr Batsuev on 11/03/14.
//  Copyright (c) 2014 Aleksandr Batsuev. All rights reserved.
//

#import "Document.h"
#import "ConvertionServer.h"
#import <AFNetworking.h>

@interface Document()

@property NSString* loadedDocument;

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

- (IBAction)showMarkdownHelp:(id)sender {
    [[NSWorkspace sharedWorkspace] openURL:[NSURL URLWithString:@"https://github.com/yogthos/markdown-clj#supported-syntax"]];
}

- (void)windowControllerDidLoadNib:(NSWindowController *)aController
{
    [super windowControllerDidLoadNib:aController];
        
    self.codeView.delegate = self;
    [self.codeView setMode:ACEModeMarkdown];
    [self.codeView setTheme:ACEThemeChrome];
    [self.codeView setShowInvisibles:NO];
    [self.codeView setFontSize:12];
    
    [self.loadingView startAnimation:nil];
    
    if (self.loadedDocument != nil) {
        self.codeView.string = self.loadedDocument;
    }

    if ([ConvertionServer server].serverAvailable) {
        [self onAvailable:nil];
    }else {
        [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(onAvailable:) name:@"ServerAvailable" object:nil];
    }
}

- (void)onAvailable:(NSNotification*)notification {
    [self.loadingView stopAnimation:nil];
    [self.loadingView setHidden:YES];
    [self updatePreview];
}

- (NSURL*)projectBase:(NSURL*)url {
    if ([[url path] isEqualToString:@"/"]) return nil;
    NSArray *files = [[NSFileManager defaultManager] contentsOfDirectoryAtPath:[url path] error:nil];
    for (NSString *file in files) {
        if ([file isEqualToString:@".git"]) return url;
    }
    return [self projectBase:[NSURL URLWithString:url.absoluteString.stringByDeletingLastPathComponent]];
}

- (void)updatePreview {
    if ([ConvertionServer server].port == nil) return;
    
    AFHTTPRequestOperationManager *manager = [AFHTTPRequestOperationManager manager];
    manager.responseSerializer = [AFHTTPResponseSerializer serializer];
    NSDictionary *parameters = @{@"data": self.codeView.string };
    NSString *url = [NSString stringWithFormat:@"http://127.0.0.1:%@/", [ConvertionServer server].port];
    
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
              string = [string stringByReplacingOccurrencesOfString:@"{{SAMPLES_BASE}}" withString:[NSString stringWithFormat:@"http://127.0.0.1:%@", [ConvertionServer server].port]];
              
              NSString *dir = [[self.fileURL absoluteString] stringByDeletingLastPathComponent];
              if (dir) {
                  NSURL *baseDir = [self projectBase:[NSURL URLWithString:dir]];
                  if (baseDir)
                      string = [string stringByReplacingOccurrencesOfString:@"{{BASE}}" withString:[baseDir path]];
              }
              NSLog(@"%@", string);
              
              [self.preview.mainFrame loadHTMLString:string baseURL:self.fileURL];
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
