//
//  Document.m
//  DocsEditor
//
//  Created by Aleksandr Batsuev on 11/03/14.
//  Copyright (c) 2014 Aleksandr Batsuev. All rights reserved.
//

#import "Document.h"

@interface Document()

@property NSString* loadedDocument;
@property NSString* tempFilePath;

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
    
    [self.textView.textStorage setFont:[NSFont fontWithName:@"Monaco" size:14]];
    self.textView.textColor = self.textView.insertionPointColor = [NSColor colorWithRed:181 green:196 blue:208 alpha:255];
    
    self.tempFilePath = [NSTemporaryDirectory() stringByAppendingPathComponent: [NSString stringWithFormat: @"%.0f.", [NSDate timeIntervalSinceReferenceDate] * 1000.0]];
    
    if (self.loadedDocument != nil) {
        self.textView.string = self.loadedDocument;
        [self updatePreview];
    }

    // Add any code here that needs to be executed once the windowController has loaded the document's window.
}

- (void)updatePreview {
    NSString *src = [self.tempFilePath stringByAppendingString:@"md"];
    NSString *preview = [self.tempFilePath stringByAppendingString:@"html"];
    
    [[NSFileManager defaultManager] createFileAtPath:src contents:[self.textView.string dataUsingEncoding:NSUTF8StringEncoding] attributes:nil];
    
    NSTask* task = [[NSTask alloc] init];
    [task setLaunchPath:@"/usr/bin/java"];
    [task setArguments:[NSArray arrayWithObjects:
                        @"-jar",
                        [[NSBundle mainBundle] pathForResource:@"editor_server-0.1.0-SNAPSHOT-standalone" ofType:@"jar"],
                        src,
                        preview, nil]];
    [task launch];
    [task waitUntilExit];
    
    [self.preview.mainFrame loadRequest:[NSURLRequest requestWithURL:[NSURL URLWithString:preview]]];
}

+ (BOOL)autosavesInPlace
{
    return YES;
}

- (NSData *)dataOfType:(NSString *)typeName error:(NSError **)outError
{
    return [self.textView.string dataUsingEncoding:NSUTF8StringEncoding];
}

- (BOOL)readFromData:(NSData *)data ofType:(NSString *)typeName error:(NSError **)outError
{
    self.loadedDocument = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
    return YES;
}

- (IBAction)reload:(id)sender {
    [self updatePreview];
}
@end
