//
//  Document.h
//  DocsEditor
//
//  Created by Aleksandr Batsuev on 11/03/14.
//  Copyright (c) 2014 Aleksandr Batsuev. All rights reserved.
//

#import <Cocoa/Cocoa.h>
#import <WebKit/WebKit.h>
#import <ACEView.h>

@interface Document : NSDocument <ACEViewDelegate>

@property (weak) IBOutlet WebView *preview;
@property (weak) IBOutlet ACEView *codeView;
@property (weak) IBOutlet NSProgressIndicator *loadingView;
- (IBAction)showMarkdownHelp:(id)sender;

@end
